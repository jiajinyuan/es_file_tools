package com.sefon.es.tool;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * <p>Description: TODO.</p>
 * <p>Copyright: Copyright(c) 2020.</p>
 * <p>Company: Sefonsoft.</p>
 * <p>CreateTime: 2018/10/25.</p>
 *
 * @author SF2121
 * @version 1.0
 */
public class EsTestDataCreate {

    public static void main(String[] arg) {
        Random r = new Random();

        File file = new File("E:\\1poctest\\ES1\\test.txt");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter fw = new FileWriter(file); BufferedWriter writer = new BufferedWriter(fw)) {

            for (int i = 1; i <= 100; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = -1; j < r.nextInt(35); j++) {
                    sb.append(RandomStringUtils.randomAlphanumeric( r.nextInt(50))).append("|");
                }
                writer.write(StringUtils.removeEnd(sb.toString(), "|")+"\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
