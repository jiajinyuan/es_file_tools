package com.sefon;

import com.sefon.es.tool.EsInsertData;

/**
 * @program: 程序主入口
 * @Date: 2018/9/26 13:55
 * @Author: yzj
 * @Description:
 */
public class Main {

    public static void main(String[] args) {
        new EsInsertData().insertDataToEs();
    }
}
