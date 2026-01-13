package org.ocr.com;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * 包名称： org.ocr.com
 * 类名称：OpenAiCodeReview
 * 类描述：程序入口
 * 创建人：@author zhengxiaolong
 * 创建时间：2026-01-12 14:51
 */
public class OpenAiCodeReview {
    public static void main(String[] args) {
        // 1.检出提交代码
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));

        try {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            StringBuilder diffCode = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                diffCode.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            System.out.println("Exit Code: " + exitCode);
            System.out.println("待评审代码 ->");
            System.out.println(diffCode);

        } catch (Exception e) {

        }
    }
}