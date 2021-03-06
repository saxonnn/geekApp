package xyz.geekweb.stock.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.geekweb.config.DataProperties;
import xyz.geekweb.stock.mq.Sender;
import xyz.geekweb.stock.pojo.savesinastockdata.RealTimeDataPOJO;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author lhao
 * @date 2018/4/25
 * 货币基金
 */
@Service
public class HBFundImpl implements FinanceData {


    private List<RealTimeDataPOJO> data;
    private List<RealTimeDataPOJO> watchData;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private DataProperties dataProperties;

    @Autowired
    public HBFundImpl(DataProperties dataProperties) {
        this.dataProperties = dataProperties;

    }

    public void fetchData(List<RealTimeDataPOJO> data) {
        final double low_monetary_funds_value = Double.parseDouble(this.dataProperties.getMap().get("MONETARY_FUNDS_VALUE").split(",")[0]);
        final double up_monetary_funds_value = Double.parseDouble(this.dataProperties.getMap().get("MONETARY_FUNDS_VALUE").split(",")[1]);

        this.data = data.stream().filter(item -> item.getFullCode().startsWith("sh511")).collect(toList());
        this.watchData = data.stream().filter(item -> item.getFullCode().startsWith("sh511") && item.getNow() <= low_monetary_funds_value).collect(toList());

    }

    @Override
    public void printInfo() {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("------------货币基金---------------\n");
        this.data.forEach(item -> sb.append(String.format("购买货币基金:%s 当前价[%7.3f] 卖出价[%7.3f] 卖量[%5.0f]%n", item.getFullCode(), item.getNow(), item.getSell1Price(), item.getSell1Num())));
        sb.append("-----------------------------------\n");
        logger.info(sb.toString());
    }

    @Override
    public void sendNotify(Sender sender) {
        // sender.sendNotify(this.watchData);
    }

    @Override
    public List<RealTimeDataPOJO> getData() {
        return this.data;
    }
}
