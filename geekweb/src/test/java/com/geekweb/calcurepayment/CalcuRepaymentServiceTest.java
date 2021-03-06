package com.geekweb.calcurepayment;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */

import org.junit.Test;
import xyz.geekweb.calcurepayment.CalcuRepaymentService;
import xyz.geekweb.calcurepayment.model.Detail;
import xyz.geekweb.calcurepayment.model.RepaymentScheduleData;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CalcuRepaymentServiceTest {


    @Test
    public void testOf1() {

        CalcuRepaymentService instance = new CalcuRepaymentService();
        RepaymentScheduleData<List<Detail>> result = instance.of("saxon", "2017-07-03", 400_000, 0.13, 11_000);
        assertNotNull(result);
        assertEquals(result.getData().size(), 47);
    }

    @Test
    public void testOf2() {

        CalcuRepaymentService instance = new CalcuRepaymentService();
        RepaymentScheduleData<List<Detail>> result = instance.of("jackluo", "2016-07-03", 10000, 0.12, 1000);
        assertNotNull(result);
        assertEquals(result.getData().size(), 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOf3() {

        new CalcuRepaymentService().of("jackluo", "2017-07-03", 10000, 0.12, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOf4() {

        new CalcuRepaymentService().of("jackluo", "2017-07-03", 10000, 0.12, 99);
    }
}
