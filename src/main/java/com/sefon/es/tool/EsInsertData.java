package com.sefon.es.tool;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * <p>Description: TODO.</p>
 * <p>Copyright: Copyright(c) 2020.</p>
 * <p>Company: Sefonsoft.</p>
 * <p>CreateTime: 2018/10/25.</p>
 *
 * @author SF2121
 * @version 1.0
 */
public class EsInsertData extends Base {

    public void insertDataToEs() {
        loadCfg("cfg.properties");
        String path = cfg.getProperty("es.file.path");
        if (!new File(path).exists()) {
            throw new IllegalArgumentException("'es.file.path' config error");
        }
        String spilt = cfg.getProperty("es.record.split");
        if (null == spilt || "".equals(spilt)) {
            throw new IllegalArgumentException("'es.record.split' config error");
        }
        Integer splitSize = Integer.parseInt(cfg.getProperty("Base.SplitSize"));

        //初始化线程池
        initThreadPool();

        List<File> files = new ArrayList<>();
        findFile(files, new File(path));

        Long count = 0L;
        BulkProcessor processor = initProcessor();
        List<String> strs= new ArrayList<>();
        Integer subCount = 0;
        long startTime = System.currentTimeMillis();
        for (File file : files) {
            FileReader reader = null;
            BufferedReader br = null;
            try {
                reader = new FileReader(file);
                br = new BufferedReader(reader);
                String str;
                while ((str = br.readLine()) != null) {

                    if("".equals(str)){
                        continue;
                    }
                    strs.add(str);
                    count++;
                    subCount ++;
                    if(subCount.equals(splitSize)){
                        this.threadPool.execute(new InsertDataTask(strs, spilt, processor));
                        subCount = 0;
                        strs = new ArrayList<>();
                    }
                }
            } catch (Exception e) {
                log.error("读取上传列表异常!", e);
            } finally {
                IOUtils.closeQuietly(br);
                IOUtils.closeQuietly(reader);
            }
        }
        if(strs.size() > 0){
            this.threadPool.execute(new InsertDataTask(strs, spilt, processor));
        }

        super.shutdownThreadPool();
        log.info("总记录数：" + count);
        log.info("处理完毕,使用时间:" + (System.currentTimeMillis() - startTime) + "毫秒");
    }

    private void findFile(List<File> files, File file) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File listFile : listFiles) {
                    findFile(files, listFile);
                }
            }
        } else {
            files.add(file);
        }
    }

    private BulkProcessor initProcessor() {
        String clusterNodes = cfg.getProperty("search.elasticsearch.cluster.nodes");
        String httpPort = cfg.getProperty("search.elasticsearch.port.http");
        String bulkActions = cfg.getProperty("search.elasticsearch.bulk.actions");
        String bulkSize = cfg.getProperty("search.elasticsearch.bulk.size");
        String bulkInterval = cfg.getProperty("search.elasticsearch.bulk.interval");
        String bulkThread = cfg.getProperty("search.elasticsearch.bulk.thread");

        final String PROTOCOL = "http";

        String[] nodeArr = clusterNodes.split(",");
        List<HttpHost> hostList = new ArrayList<>();
        for (String node : nodeArr) {
            HttpHost httpHost = new HttpHost(node, Integer.parseInt(httpPort), PROTOCOL);
            hostList.add(httpHost);
        }
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(hostList.toArray(new HttpHost[hostList.size()])));

        BulkProcessor bulkProcessor;
        //生成批量处理
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                log.debug("[executionId:{}]Executing bulk with {} requests", executionId, request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    log.error("[executionId:{}]" + response.buildFailureMessage(), executionId);
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("[executionId:" + executionId + "]Failed to execute bulk", failure);
            }
        };

        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);

        BulkProcessor.Builder builder = BulkProcessor.builder(bulkConsumer, listener);
        builder.setBulkActions(Integer.parseInt(bulkActions)); //达到此条数批量提交
        builder.setBulkSize(new ByteSizeValue(Integer.parseInt(bulkSize), ByteSizeUnit.MB)); //达到此大小进行批量提交
        builder.setFlushInterval(TimeValue.timeValueSeconds(Integer.parseInt(bulkInterval))); //达到此时间进行批量提交
        builder.setConcurrentRequests(Integer.parseInt(bulkThread)); //批量提交线程数

        bulkProcessor = builder.build();

        return bulkProcessor;
    }
}
