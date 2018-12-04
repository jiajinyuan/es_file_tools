package com.sefon.es.tool;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: TODO.</p>
 * <p>Copyright: Copyright(c) 2020.</p>
 * <p>Company: Sefonsoft.</p>
 * <p>CreateTime: 2018/10/25.</p>
 *
 * @author SF2121
 * @version 1.0
 */
public class InsertDataTask implements Runnable {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private BulkProcessor processor;

    private List<String> strs;

    private String split;

    private String[] cols = {"pub_stmt_resc_cde", "title", "body", "link", "linkhash", "pstd_link", "pstd_linkhash", "webs_name", "chnl_name", "pstd_indc", "authr", "smry", "key_word", "websgrpn_id", "dft_src", "prs_dmtd", "webs_cde", "chnl_cde", "visit_vol", "comt_vol", "fwdcnt", "issue_time", "found_time", "miblg_cde", "fwd_orig_miblg_cde", "miblg_acct_nbr", "fwd_orig_miblg_acct_nbr", "rmr_indc", "cntnthash", "webs_clsf_cde", "prvc_cde", "autht_indc", "key_word_dlr", "impot_dgr", "init_impot_lvl_value", "data_op_net_flg"};

    public InsertDataTask(List<String> strs, String split, BulkProcessor processor) {
        this.strs = strs;
        this.split = split;
        this.processor = processor;
    }

    public InsertDataTask(List<String> strs, String split) {
        this.strs = strs;
        this.split = split;
    }

    @Override
    public void run() {
        for (String s : strs) {
            String[] split = s.split(this.split);
            Map<String, Object> docAttr = new HashMap<>();
            for (int i = 0; i < cols.length; i++) {
                if (i >= split.length) {
                    docAttr.put(cols[i], null);
                } else {
                    docAttr.put(cols[i], split[i]);
                }
            }
            IndexRequest request = new IndexRequest("jjy", "SDC").source(docAttr);
            processor.add(request);
        }
        log.error(" ||  " + Thread.currentThread().getName() + "   " + strs.size());
    }
}
