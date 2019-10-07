package com.example.zhuashuju;

import com.example.zhuashuju.zhua.service.Zhua7mService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ZhuashujuApplicationTests {
	@Resource
	private Zhua7mService service;

	@Test
	public void testZhua() {
		// 抓取规则1
		service.zhua(2);
	}

	@Test
	public void testZhuaDataAndCreateFile() {
		// 抓取规则2
		service.zhuaDataAndCreateFile(2);
	}



}
