package com.Edo_perfume.ScentOfASA;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class ScentOfAsaApplicationTests {

	@MockBean
	private StoreHolidayService storeHolidayService;

	@Test
	void contextLoads() {
	}

}
