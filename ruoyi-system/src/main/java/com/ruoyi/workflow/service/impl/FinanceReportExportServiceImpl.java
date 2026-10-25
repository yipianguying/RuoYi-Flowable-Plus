package com.ruoyi.workflow.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.ruoyi.workflow.domain.dto.FinanceExportDTO;
import com.ruoyi.workflow.service.FinanceReportExportService;
import com.ruoyi.workflow.domain.BizPurchaseOrder;
import com.ruoyi.workflow.domain.BizSupplier;
import com.ruoyi.workflow.mapper.BizPurchaseOrderMapper;
import com.ruoyi.workflow.mapper.BizSupplierMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

@Slf4j
@Service
public class FinanceReportExportServiceImpl implements FinanceReportExportService {

    @Resource
    private BizPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private BizSupplierMapper supplierMapper;

    /*
     * =====================================================================================
     * JUC 考点 1：自定义业务线程池 (ThreadPoolExecutor)
     * 【底层原理】：线程池的核心隔离机制。必须使用有界队列，绝对不能使用 Executors 默认方法（默认是 Integer.MAX_VALUE 无界队列，极易 OOM）。
     * 【面试防守】：为什么参数这样设置？
     *  - 核心线程 4：因为我们是 2C4G 的低配服务器，IO密集型任务（查MySQL），设置为 CPU核数 * 2 = 4。
     *  - 最大线程 8：当并发分页抓取突增时，最多允许 8 个并发，防止把数据库连接池（如 HikariCP）耗尽。
     *  - CallerRunsPolicy (调用者运行拒绝策略)：如果 500 容量的队列满了，不抛异常丢弃数据，而是让主线程（Tomcat Web 线程）亲自去执行分页查询。这不仅能保证数据不丢，还能利用“背压（Backpressure）”机制天然阻塞前端进一步的请求！
     * =====================================================================================
     */
    private final ThreadPoolExecutor exportThreadPool = new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(500),
        Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /*
     * =====================================================================================
     * JUC 考点 2：Semaphore (信号量) - 服务器全局硬件限流
     * 【底层原理】：基于 AQS (AbstractQueuedSynchronizer) 的共享锁实现。通过维护一个 state 变量，每次 tryAcquire() 通过 CAS 将 state 减 1，release() 加 1。
     * 【面试防守】：为什么设置为 3？
     *  - 保护低配主机：2C4G 内存极其珍贵。哪怕 EasyExcel 做了分片流式，一个导出任务在 JVM 堆内存也会驻留十几 MB 到几十 MB 的临时缓冲。
     *  - 如果同时有 10 个财务同时点导出，服务器直接 OOM。限制同时只能有 3 个导出任务在跑，第 4 个直接快速失败（Fail-fast），提示“稍后再试”，极大地保护了工厂的核心审批流主流程！
     * =====================================================================================
     */
    private final Semaphore exportSemaphore = new Semaphore(3);

    /*
     * =====================================================================================
     * JUC 考点 3：LongAdder 替代 AtomicInteger - 高并发计数器
     * 【底层原理 (必问)】：AtomicInteger 底层是单一的 volatile int 加上 CAS（Compare-And-Swap）自旋。如果多个线程并发加 1，只有一个成功，剩下的全在死循环自旋，白白浪费 CPU。
     *  - LongAdder 采用了“分散热点 (Striped64)”的设计思想：底层有一个 Base 核心变量和一个 Cell[] 数组。
     *  - 在并发低时，直接通过 CAS 操作 Base 变量；
     *  - 当并发极高发生冲突时，通过线程的 Hash 值将数据映射到不同的 Cell 桶里面去各自独立自增！
     *  - 最终调用 sum() 时，把 Base + 所有 Cell 的值累加返回。空间换时间，大幅降低 CPU 自旋开销！
     * =====================================================================================
     */
    private final LongAdder totalProcessedCount = new LongAdder();

    /*
     * =====================================================================================
     * JUC 考点 4：ThreadLocal - 线程上下文隔离与 JVM 内存泄露防御
     * 【底层原理 (高频拷打)】：每个 Thread 对象内部都有一个 `ThreadLocal.ThreadLocalMap` 属性。
     *  - ThreadLocalMap 的 Key 是 ThreadLocal 对象的 **弱引用 (WeakReference)**，Value 是我们存入的对象（强引用）。
     *  - 【为什么会内存泄露？】：当由于垃圾回收导致 Key (弱引用) 被 JVM 回收变成 null 后，Value 依然后存在 `Thread -> ThreadLocalMap -> Entry(null, value)` 的强引用链！如果线程池里的线程一直复用不死，这个 Value 永远无法被 GC 回收！
     *  - 【最佳实践】：所以在多线程、线程池场景下，使用完 ThreadLocal 必须在 `finally` 块中手动显式调用 `remove()`！
     * =====================================================================================
     */
    private static final ThreadLocal<Map<String, Object>> EXPORT_CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(HashMap::new);

    /*
     * =====================================================================================
     * JUC 考点 5：StampedLock (乐高般的最强锁) - 读多写少场景下的极致缓存锁
     * 【底层原理】：相比 ReentrantReadWriteLock (读写锁，读锁和写锁依然互斥)，StampedLock 引入了 **“乐观读 (Optimistic Read)”**。
     *  - 乐观读完全不加任何互斥锁！它只记录一个戳 (Stamp 版本号)。
     *  - 读数据时直接裸读，读完后调用 `validate(stamp)` 检查这段时间内有没有写操作发生过。
     *  - 如果发现被写线程插队修改了，才“升级”为真实的悲观读锁再去读一次。大幅减少了因读锁阻塞写锁、或频繁加锁解锁的上下文切换消耗！
     * 【面试防守】：工厂供应商档案一般建好就几年不变（读多写极少），把它从 MySQL 缓存到 local cache 并用 StampedLock 保护，能将连表查询降维成纯内存读取，速度提升 100 倍！
     * =====================================================================================
     */
    private final StampedLock supplierCacheLock = new StampedLock();
    private final Map<Long, BizSupplier> supplierLocalCache = new HashMap<>();

    /*
     * =====================================================================================
     * JUC 考点 6：ReentrantLock + Condition - 生产者/消费者中的显式条件挂起
     * 【底层原理】：ReentrantLock 基于 AQS 独占锁，Condition (条件变量) 替代了原生的 Object.wait() / notify()。
     *  - 调用 `condition.await()` 会主动释放当前获得的 ReentrantLock 锁，并把当前线程放入 Condition 的等待队列（单向链表）中挂起 (park)。
     *  - 当别的线程调用 `condition.signalAll()` 时，会将线程从 Condition 队列移动到 AQS 核心阻塞队列中，重新竞争锁并唤醒 (unpark)。
     * 【业务作用】：运维级熔断开关。如果运维监控监控到导出导致 MySQL 飙升，可点击后台按钮挂起所有写入线程，等系统平稳再唤醒，防止工厂核心生产链路宕机！
     * =====================================================================================
     */
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpausedCondition = pauseLock.newCondition();
    private volatile boolean isPaused = false; // volatile 保证多线程对布尔标志位的内存可见性，防止线程使用本地 CPU 缓存

    /**
     * 系统启动时，将热点静态数据加载到本地内存（避免导出时频繁连表 JOIN）
     */
    @PostConstruct
    public void initSupplierCache() {
        long stamp = supplierCacheLock.writeLock(); // 获取独占写锁
        try {
            List<BizSupplier> list = supplierMapper.selectAll();
            list.forEach(s -> supplierLocalCache.put(s.getId(), s));
            log.info("本地热点缓存加载成功，共锁定 {} 条供应商档案", list.size());
        } finally {
            supplierCacheLock.unlockWrite(stamp); // 绝对锁规则：在 finally 中释放
        }
    }

    /**
     * 【面试必考点】：演示 StampedLock 乐观读的完整标准编码范式
     */
    private BizSupplier getSupplierFromCacheSafe(Long supplierId) {
        // 第一步：直接获取“乐观读戳” (无锁，速度极快)
        long stamp = supplierCacheLock.tryOptimisticRead();
        // 第二步：直接从内存中读取数据 (此时可能有其他写线程正在修改 map)
        BizSupplier supplier = supplierLocalCache.get(supplierId);

        // 第三步：校验在上述读取期间，有没有写锁介入操作过
        if (!supplierCacheLock.validate(stamp)) {
            // 第四步：如果 validate = false，说明数据可能被改过了，产生脏读风险！
            // 此时必须“升级”为传统的悲观读锁，老老实实挂起等待写锁完成
            stamp = supplierCacheLock.readLock();
            try {
                supplier = supplierLocalCache.get(supplierId);
            } finally {
                supplierCacheLock.unlockRead(stamp);
            }
        }
        return supplier;
    }

    /**
     * 核心导出方法 - 【生产者-消费者模型 + 内存分片流式写入】
     */
    @Override
    public void exportFinanceReconciliationReport(HttpServletResponse response, String yearMonth, String currentOperator) throws IOException {
        // 1. 硬件级防护：尝试获取信号量。拿到则执行，拿不到说明服务器正在处理 3 个重度导出，立马抛错拒绝服务！
        if (!exportSemaphore.tryAcquire()) {
            throw new RuntimeException("当前导出报表并发已达到服务器上限（3个），为保护生产车间系统运行，请稍后再试！");
        }

        totalProcessedCount.reset();
        // 阻塞队列做“数据管道”：最大只存 100 个批次，利用有界队列的阻塞特性，自然限制生产者并发查询 MySQL 的速度！
        BlockingQueue<List<FinanceExportDTO>> dataBufferQueue = new ArrayBlockingQueue<>(100);
        int pageSize = 2000;
        int totalRecords = purchaseOrderMapper.countByYearMonth(yearMonth);
        int totalPages = (totalRecords + pageSize - 1) / pageSize;

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode("邹城工厂_" + yearMonth + "_对账单", "UTF-8") + ".xlsx");

        // 【EasyExcel 核心优越性】：为什么不用传统的 POI？
        // Apache POI 会把整个 Excel 的百万行 DOM 结构直接一次性在内存中构建出 Java 对象，立刻 OOM！
        // EasyExcel 底层采用了将 SAX 模式与文件顺序追加写入结合的方案：内存里只驻留几十行缓存，写完即释放，大幅度降低堆内存占用！
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), FinanceExportDTO.class).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("财务明细").build();

        try {
            List<CompletableFuture<Void>> producerFutures = new ArrayList<>();

            // 循环多线程，让子线程作为【生产者】去 MySQL 分页拉取数据
            for (int page = 1; page <= totalPages; page++) {
                final int currentPage = page;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 【ThreadLocal 注入】：为当前任务子线程绑定会话上下文
                        Map<String, Object> context = EXPORT_CONTEXT_THREAD_LOCAL.get();
                        context.put("operator", currentOperator);
                        context.put("startTime", System.currentTimeMillis());

                        // 从 MySQL 查当页数据（避免一次性抓取全部）
                        List<BizPurchaseOrder> orders = purchaseOrderMapper.selectPageByYearMonth(yearMonth, (currentPage - 1) * pageSize, pageSize);
                        List<FinanceExportDTO> dtoList = new ArrayList<>();

                        for (BizPurchaseOrder order : orders) {
                            FinanceExportDTO dto = new FinanceExportDTO();
                            dto.setOrderNo(order.getOrderNo());
                            dto.setTotalAmount(order.getTotalAmount());

                            // 【查本地缓存不查库】：利用 StampedLock 内存抓取供应商名字，将 耗时慢的 MySQL JOIN 查询，变成极快的内存 KV 匹配！
                            BizSupplier supplier = getSupplierFromCacheSafe(order.getSupplierId());
                            dto.setSupplierName(supplier != null ? supplier.getSupplierName() : "未知");
                            dto.setBankAccount(supplier != null ? supplier.getBankAccount() : "无");
                            dtoList.add(dto);
                        }

                        // 把处理好的当页数据放入有界阻塞队列。如果队列满了（主线程写 Excel 慢了），子线程就会被阻塞在这里 (put 方法)，这就叫【背压】
                        dataBufferQueue.put(dtoList);
                        // LongAdder 分布式并发计数
                        totalProcessedCount.add(dtoList.size());

                        long cost = System.currentTimeMillis() - (long) EXPORT_CONTEXT_THREAD_LOCAL.get().get("startTime");
                        log.info("操作人 [{}] 的第 {} 页抓取完毕，耗时 {} ms", EXPORT_CONTEXT_THREAD_LOCAL.get().get("operator"), currentPage, cost);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 重新设立中断标志位
                        log.error("分页抓取线程被中断", e);
                    } finally {
                        // 🔥🔥🔥【绝杀防护】：使用完必须 remove！必须 remove！必须 remove！
                        // 否则在线程池长期运行中，该子线程再执行下一个任务时，还会拿到上一个人的 context 脏数据，并导致内存泄露！
                        EXPORT_CONTEXT_THREAD_LOCAL.remove();
                    }
                }, exportThreadPool);

                producerFutures.add(future);
            }

            // 监听所有分页子线程全部执行完成
            CompletableFuture.allOf(producerFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    try {
                        // 【精妙设计：毒药丸（Poison Pill）】：放入一个空集合作为结束信号，告诉【消费者】数据已经生产完毕，可以安全下班了
                        dataBufferQueue.put(new ArrayList<>());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

            // ================= 主线程作为【消费者】：循环从阻塞队列拉取数据，同步流式写盘 =================
            // 为什么写入不在多线程里做？
            // 答：因为文件的底层磁盘 IO 写入是线性的！如果我们用多线程同时往同一个流 (OutputStream) 里追加数据，必定导致文件格式损坏、数据乱序或死锁！所以必须“多线程生产 + 单线程消费写盘”！
            while (true) {
                // 每次消费写盘前，检查是否有系统紧急熔断暂停信号
                checkAndBlockIfPaused();

                // 阻塞式拉取，队列没数据时，主线程自动挂起等待，不浪费 CPU
                List<FinanceExportDTO> batchData = dataBufferQueue.take();
                if (batchData.isEmpty()) {
                    break; // 遇到了我们的“毒药丸（空列表）”，说明所有数据均已写完，跳出循环！
                }

                // 调用 EasyExcel 流式追加写盘
                excelWriter.write(batchData, writeSheet);
            }
            log.info("报表导出完成！共流式写盘 {} 条，最终总计数: {}", totalProcessedCount.sum(), totalProcessedCount.longValue());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("导出被打断");
        } finally {
            // 关闭流
            if (excelWriter != null) excelWriter.finish();
            // 务必释放信号量，归还系统并发额度！
            exportSemaphore.release();
        }
    }

    /**
     * ReentrantLock + Condition 的运维监控干预：线程挂起与恢复
     */
    private void checkAndBlockIfPaused() throws InterruptedException {
        pauseLock.lock();
        try {
            while (isPaused) {
                log.warn("检测到系统触发了紧急熔断暂停，当前导出消费线程正挂起等待...");
                // 释放锁并进入 Condition 的等待队列沉睡，直到被其他线程调用 signalAll() 唤醒
                unpausedCondition.await();
            }
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * 外部给系统监控大屏对接的接口：
     * 如果监控脚本发现 JVM Old 区内存使用超过 85%，自动调用此接口传入 true；降下来后传入 false。
     */
    public void togglePause(boolean pause) {
        pauseLock.lock();
        try {
            this.isPaused = pause;
            if (!pause) {
                unpausedCondition.signalAll(); // 唤醒所有沉睡的消费者线程继续干活
                log.info("系统熔断解除，报表导出线程已被唤醒继续执行！");
            }
        } finally {
            pauseLock.unlock();
        }
    }
}
