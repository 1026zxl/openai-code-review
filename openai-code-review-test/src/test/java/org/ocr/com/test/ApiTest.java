package org.ocr.com.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 包名称： org.ocr.com.test
 * 类名称：ApiTest
 * 类描述：TODO
 * 创建人：@author zhengxiaolong
 * 创建时间：2026-01-12 15:30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {
        log.info("测试评审V2... ...");
        log.info("测试评审V3... ...");
        log.info("测试评审V4... ...");
        log.info("测试评审V5... ...");
        log.info("测试评审V6... ...");
    }
}