package com.sefon.es.tool;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @program: poc_file_tools
 * @Date: 2018/9/26 14:44
 * @Author: yzj
 * @Description:
 */
public class Base {
    protected Logger log;
    private String cfgPath;
    protected Properties cfg = new Properties();//配置文件信息
    protected ExecutorService threadPool = null;

    public Base() {
        //处理配置文件路径
        cfgPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (cfgPath != null) {
            if (cfgPath.toLowerCase().endsWith(".jar")) {
                cfgPath = cfgPath.substring(0, cfgPath.lastIndexOf("/") + 1);
            }
            System.setProperty("PROP_SDC_SDK_CONFIG", cfgPath);//SDK需要指定外部配置文件地址
        }
        //处理log4j配置文件路径
        String log4jCfgPath = cfgPath + "log4j.properties";
        boolean userInnerLog4j = true;
        if (new File(log4jCfgPath).exists()) {
            PropertyConfigurator.configure(log4jCfgPath);
            userInnerLog4j = false;
        }
        log = LoggerFactory.getLogger(this.getClass());
        if (userInnerLog4j) {
            log.info("log4j使用jar包内部配置文件");
        } else {
            log.info("log4j使用jar包外部配置文件: " + log4jCfgPath);
        }
    }

    protected void loadCfg(String cfgFileName) {
        log.info("开始读取配置文件");
        //1.获取配置文件路径，在当前jar包同级目录
        InputStream cfgInptStream = null;
        try {
            log.info("查找jar外部配置文件");
            String cfgFile = "";
            if (cfgPath != null) {
                cfgFile = cfgPath + cfgFileName;
            }
            if (new File(cfgFile).exists()) {
                cfgInptStream = new FileInputStream(cfgFile);
            } else {
                log.error("未找到外部配置文件,查找jar内部配置文件");
                cfgFile = null;
                URL cfgPathUrl = this.getClass().getClassLoader().getResource(cfgFileName);
                if (cfgPathUrl != null) {
                    cfgFile = cfgPathUrl.getPath();
                    cfgInptStream = cfgPathUrl.openStream();
                }
            }
            if (cfgFile == null) {
                log.error("未找到任何配置文件,退出");
                System.exit(-1);
            }
            log.info("配置文件路径:" + cfgFile);
            cfg.clear();
            cfg.load(cfgInptStream);
            log.info("配置文件读取完毕");
        } catch (Exception e) {
            log.error("配置文件加载异常:", e);
            System.exit(-1);
        } finally {
            IOUtils.closeQuietly(cfgInptStream);
        }
    }

    /**
     * 关闭线程池
     **/
    protected void shutdownThreadPool() {
        threadPool.shutdown();
        while (true) {
            if (threadPool.isTerminated()) {
                break;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {

            }
        }
    }


    private void resetThreadPool() {
        //重置线程池
        if (this.threadPool != null) {
            this.threadPool.shutdown();
            this.threadPool = null;
        }
    }


    protected void initThreadPool() {
        if (this.threadPool != null) {
            log.info("线程池已经初始化");
            resetThreadPool();
        }
        String poolSizeMin = cfg.getProperty("Base.ThreadPoolSize.min", "10");
        String poolSizeMax = cfg.getProperty("Base.ThreadPoolSize.max", "40");
        String poolQueue = cfg.getProperty("Base.ThreadPoolSize.Queue", "2048");

        log.info("初始化线程池,线程最小执行个数:" + poolSizeMin + ",线程最大执行个数" + poolSizeMax);
        int sizeMin = Integer.valueOf(poolSizeMin);
        int sizeMax = Integer.valueOf(poolSizeMax);
        if (sizeMin > sizeMax) {
            sizeMin = 10;
            sizeMax = 40;
            log.error("参数错误，重置为最小执行" + sizeMin + "个线程，最大执行" + sizeMax + "个线程");
        }
        // int thread_count = Runtime.getRuntime().availableProcessors(); //cpu核心数量
        //参照Executors.newFixedThreadPool(size);
        this.threadPool = new ThreadPoolExecutor(sizeMin, sizeMax, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(Integer.parseInt(poolQueue)), new BlockingRejectedExecutionHandler());

    }

    /**
     * 自定义拒绝执行策略：阻塞式地将任务添加至工作队列中
     *
     * @author hasee
     *
     */
    private class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                try {
                    // 使用阻塞方法向工作队列中添加任务
                    executor.getQueue().put(r);
                } catch (InterruptedException e) {
                    executor.execute(r);
                }
            }
        }
    }

}
