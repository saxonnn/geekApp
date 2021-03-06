package xyz.geekweb.stock.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import xyz.geekweb.config.DataProperties;
import xyz.geekweb.stock.pojo.KZZBean;
import xyz.geekweb.stock.mq.Sender;
import xyz.geekweb.stock.pojo.savesinastockdata.RealTimeDataPOJO;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author lhao
 * @date 2018/4/25
 * 可转债,元和
 */
@Service
@Slf4j
public class KZZImpl implements FinanceData {

    private List<RealTimeDataPOJO> data;
    private List<RealTimeDataPOJO> watchData;

    private DataProperties dataProperties;

    private static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");

    @Autowired
    public KZZImpl(DataProperties dataProperties) {
        this.dataProperties = dataProperties;
    }

    public void fetchData(List<RealTimeDataPOJO> realTimeDataPOJO) {

        final double low_132003_value = Double.parseDouble(this.dataProperties.getMap().get("132003_VALUE").split(",")[0]);
        final double up_132003_value = Double.parseDouble(this.dataProperties.getMap().get("132003_VALUE").split(",")[1]);

        final double low_505888_value = Double.parseDouble(this.dataProperties.getMap().get("505888_VALUE").split(",")[0]);
        final double up_505888_value = Double.parseDouble(this.dataProperties.getMap().get("505888_VALUE").split(",")[1]);

        this.data = realTimeDataPOJO.stream().filter(item -> (item.getFullCode().startsWith("sh505888") || item.getFullCode().startsWith("sh132003"))).collect(toList());

        this.watchData = realTimeDataPOJO.stream().filter(item ->
                ((item.getFullCode().startsWith("sh505888") && item.getNow() <= low_505888_value) || (item.getFullCode().startsWith("sh132003") && item.getNow() <= low_132003_value))).collect(toList());

    }


    /**
     * 可转债溢价率监测
     * @param realTimeDataPOJO
     */
    public void fetchKZZData(List<RealTimeDataPOJO> realTimeDataPOJO) {

        List<String> codeList=new ArrayList<>();
        String[] kzzes = this.dataProperties.getMap().get("kzz").split(";");
        for (String kzz: kzzes) {
            log.debug("parse:{}",kzz);
            String[] codes = kzz.split(":");
            Assert.isTrue(codes.length>0,"must be 可转债代码:股票代码:转股价; format");
            List<RealTimeDataPOJO> searchResult = realTimeDataPOJO.stream().filter(item -> (item.getFullCode().startsWith(codes[0]) || item.getFullCode().startsWith(codes[1]))).collect(toList());
            Assert.isTrue(searchResult.size()==2,"must be two items or not exist");
            DecimalFormat dfNum = new DecimalFormat("#0");
            float basePrice = Float.parseFloat(codes[2]);
            String fullCode = searchResult.get(0).getFullCode();
            double stockBuy1Price = searchResult.get(1).getBuy1Price();
            int stockBuy1Num = (int)(searchResult.get(1).getBuy1Num()/100);

            boolean isSH = fullCode.startsWith("sh");
            boolean isSZ = fullCode.startsWith("sz");

            //取最大可买到值
            int kzzSellNum =0;
            double kzzSellPrice = 0.0d;
            int min = 10;
            if(isSZ){
                min = 100;
            }
            if(searchResult.get(0).getSell1Num()> min){
                log.debug("try sell1：{}",searchResult.get(0).getSell1Num());
                kzzSellNum =(int)(searchResult.get(0).getSell1Num());
                kzzSellPrice = searchResult.get(0).getSell1Price();
            }else if(searchResult.get(0).getSell2Num()>min){
                log.debug("try sell2：{}",searchResult.get(0).getSell2Num());
                kzzSellNum =(int)(searchResult.get(0).getSell2Num());
                kzzSellPrice = searchResult.get(0).getSell2Price();
            }else if(searchResult.get(0).getSell3Num()>min){
                log.debug("try sell3：{}",searchResult.get(0).getSell3Num());
                kzzSellNum =(int)(searchResult.get(0).getSell3Num());
                kzzSellPrice = searchResult.get(0).getSell3Price();
            }else if(searchResult.get(0).getSell4Num()>min){
                log.debug("try sell4：{}",searchResult.get(0).getSell4Num());
                kzzSellNum =(int)(searchResult.get(0).getSell4Num());
                kzzSellPrice = searchResult.get(0).getSell4Price();
            }else if(searchResult.get(0).getSell5Num()>min){
                log.debug("try sell5：{}",searchResult.get(0).getSell5Num());
                kzzSellNum =(int)(searchResult.get(0).getSell5Num());
                kzzSellPrice = searchResult.get(0).getSell5Price();
            }


            double diffPercent=(((kzzSellPrice/100*basePrice)-stockBuy1Price)/stockBuy1Price)*100;

            log.debug("[{}:{}] {}% buy[{}:{}] sell[{}:{}]", codes[0],codes[1], decimalFormat.format(diffPercent),kzzSellNum, kzzSellPrice,stockBuy1Num,stockBuy1Price);
            if(diffPercent<1.0d && kzzSellNum > min && stockBuy1Num>100){
                ///log.warn("[{}:{}] {}% buy[{}:{}] sell[{}:{}]", codes[0],codes[1], decimalFormat.format(diffPercent),kzzSellNum, kzzSellPrice,stockBuy1Num,stockBuy1Price);
            }
        }

    }

    @Override
    public void printInfo() {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("--------------可转债--------------\n");
        this.data.forEach(item -> sb.append(String.format("%-6s 当前价[%7.3f] 卖出价[%7.3f] 卖量[%5.0f] %-6s %n", item.getFullCode(), item.getNow(), item.getSell1Price(), item.getSell1Num(), item.getName())));
        sb.append("--------------------------------------\n");
        log.info(sb.toString());
    }

    @Override
    public void sendNotify(Sender sender) {
        // sender.sendNotify(this.watchData);
    }

    @Override
    public List<RealTimeDataPOJO> getData() {
        return this.data;
    }


    /**
     * 可转债折价套利（卖股买债）
     * @param realTimeDataPOJO
     */
    public void SellStockAndBuyKzz(List<RealTimeDataPOJO> realTimeDataPOJO) {

        List<String> codeList=new ArrayList<>();
        //监测额可转债列表
        String[] kzzes = this.dataProperties.getMap().get("kzz").split(";");
        for (String kzz: kzzes) {
            log.debug("parse:{}",kzz);
            String[] codes = kzz.split(":");
            Assert.isTrue(codes.length>0,"must be 可转债代码:股票代码:转股价; format");
            List<RealTimeDataPOJO> searchResult = realTimeDataPOJO.stream().filter(item -> (item.getFullCode().startsWith(codes[0]) || item.getFullCode().startsWith(codes[1]))).collect(toList());
            Assert.isTrue(searchResult.size()==2,"must be two items or not exist");
            DecimalFormat dfNum = new DecimalFormat("#0");
            float basePrice = Float.parseFloat(codes[2]);
            String fullCode = searchResult.get(0).getFullCode();
            double stockBuy1Price = searchResult.get(1).getBuy1Price();
            int stockBuy1Num = (int)(searchResult.get(1).getBuy1Num()/100);

            boolean isSH = fullCode.startsWith("sh");
            boolean isSZ = fullCode.startsWith("sz");

            //取最大可买到值
            int kzzBuyNum =0;
            double kzzBuyPrice = 0.0d;
            int min = 10;
            if(isSZ){
                min = 100;
            }
            if(searchResult.get(0).getBuy1Num()> min){
                log.debug("try buy1：{}",searchResult.get(0).getBuy1Num());
                kzzBuyNum =(int)(searchResult.get(0).getBuy1Price());
                kzzBuyPrice = searchResult.get(0).getSell1Price();
            }else if(searchResult.get(0).getBuy2Num()>min){
                log.debug("try buy2：{}",searchResult.get(0).getBuy2Num());
                kzzBuyNum =(int)(searchResult.get(0).getSell2Num());
                kzzBuyPrice = searchResult.get(0).getBuy2Price();
            }else if(searchResult.get(0).getBuy3Num()>min){
                log.debug("try buy3：{}",searchResult.get(0).getBuy3Num());
                kzzBuyNum =(int)(searchResult.get(0).getSell3Num());
                kzzBuyPrice = searchResult.get(0).getBuy3Price();
            }else if(searchResult.get(0).getBuy4Num()>min){
                log.debug("try buy4：{}",searchResult.get(0).getBuy4Num());
                kzzBuyNum =(int)(searchResult.get(0).getSell4Num());
                kzzBuyPrice = searchResult.get(0).getBuy4Price();
            }else if(searchResult.get(0).getBuy5Num()>min){
                log.debug("try buy5：{}",searchResult.get(0).getSell5Num());
                kzzBuyNum =(int)(searchResult.get(0).getBuy5Num());
                kzzBuyPrice = searchResult.get(0).getBuy5Price();
            }


            double diffPercent=(((kzzBuyPrice/100*basePrice)-stockBuy1Price)/stockBuy1Price)*100;
            if(diffPercent<-0.1d){
                log.info("[{}:{}] {}% sell[{}:{}] buy[{}:{}]", codes[0],codes[1], decimalFormat.format(diffPercent),kzzBuyNum, kzzBuyPrice,stockBuy1Num,stockBuy1Price);
            }
            if(diffPercent<-0.4d && kzzBuyNum > min && stockBuy1Num>10){
                log.warn("[{}:{}] {}% sell[{}:{}] buy[{}:{}]", codes[0],codes[1], decimalFormat.format(diffPercent),kzzBuyNum, kzzBuyPrice,stockBuy1Num,stockBuy1Price);
            }
        }

    }

    /**
     * 可转债折价套利（卖股买债）
     * @param realTimeDataPOJO
     */
    public List<KZZBean> getKzzBean(List<RealTimeDataPOJO> realTimeDataPOJO, String[] kzzes) {

        List<KZZBean> result = new ArrayList<>();
        KZZBean bean=null;
        for (String kzz: kzzes) {
            bean = new KZZBean();
            String[] codes = kzz.split(":");
            Assert.isTrue(codes.length>0,"must be 可转债代码:股票代码:转股价; format");
            List<RealTimeDataPOJO> searchResult = realTimeDataPOJO.stream().filter(item -> (item.getFullCode().startsWith(codes[0]) || item.getFullCode().startsWith(codes[1]))).collect(toList());
            Assert.isTrue(searchResult.size()==2,"must be two items or not exist");
            DecimalFormat dfNum = new DecimalFormat("#0");
            float basePrice = Float.parseFloat(codes[2]);
            String fullCode = searchResult.get(0).getFullCode();
            double stockBuy1Price = searchResult.get(1).getBuy1Price();
            int stockBuy1Num = (int)(searchResult.get(1).getBuy1Num()/100);

            boolean isSH = fullCode.startsWith("sh");
            boolean isSZ = fullCode.startsWith("sz");

            //取最大可买到值
            int buyNum =0;
            double buyPrice = 0.0d;
            double buyAmount = 0.0d;
            String buyType="";
            int min = 10;
            if(isSZ){
                min = 100;
            }
            if(searchResult.get(0).getBuy1Num()> min){
                buyType = "buy1";
                buyNum =(int)(searchResult.get(0).getSell1Num());
                buyPrice = searchResult.get(0).getSell1Price();
            }else if(searchResult.get(0).getBuy2Num()>min){
                buyType = "buy2";
                buyNum =(int)(searchResult.get(0).getSell2Num());
                buyPrice = searchResult.get(0).getBuy2Price();
            }else if(searchResult.get(0).getBuy3Num()>min){
                buyType = "buy3";
                buyNum =(int)(searchResult.get(0).getSell3Num());
                buyPrice = searchResult.get(0).getBuy3Price();
            }else if(searchResult.get(0).getBuy4Num()>min){
                buyType = "buy4";
                buyNum =(int)(searchResult.get(0).getSell4Num());
                buyPrice = searchResult.get(0).getBuy4Price();
            }else if(searchResult.get(0).getBuy5Num()>min){
                buyType = "buy5";
                buyNum =(int)(searchResult.get(0).getBuy5Num());
                buyPrice = searchResult.get(0).getBuy5Price();
            }

            double diffPercent=(((buyPrice/100*basePrice)-stockBuy1Price)/stockBuy1Price)*100;
            double formatDiffPercent = Double.parseDouble(String.format("%.2f",diffPercent));
            bean.setDiffPercent(formatDiffPercent);
            bean.setBuyType(buyType);
            bean.setBuyNum(buyNum);
            bean.setBuyPrice(buyPrice);
            buyAmount = buyPrice * buyNum;
            bean.setBuyAmount(buyAmount);
            bean.setInput(kzz);
            bean.setNow(new Date());
            result.add(bean);
        }
        return result;

    }
}
