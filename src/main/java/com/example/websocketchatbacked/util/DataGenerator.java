package com.example.websocketchatbacked.util;

import com.example.websocketchatbacked.entity.*;
import com.example.websocketchatbacked.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@ConditionalOnProperty(name = "data.generator.enabled", havingValue = "true", matchIfMissing = false)
public class DataGenerator implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserNumberRepository userNumberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RechargeRepository rechargeRepository;

    @Autowired
    private ProductRateRepository productRateRepository;

    @Autowired
    private PromotionRuleRepository promotionRuleRepository;

    @Autowired
    private UserNumberProductRepository userNumberProductRepository;

    @Autowired
    private PackageDetailRepository packageDetailRepository;

    @Autowired
    private UsageDetailRepository usageDetailRepository;

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private FileOperationLogRepository fileOperationLogRepository;

    private final Random random = new Random();

    private final String[] REAL_NAMES = {"张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十", "钱十一", "陈十二"};
    private final String[] ID_TYPES = {"身份证", "护照", "军官证", "港澳通行证"};
    private final String[] PHONE_PREFIXES = {"138", "139", "150", "151", "152", "186", "187", "188", "189"};
    private final String[] PRODUCT_NAMES = {"基础套餐", "流量包", "语音包", "短信包", "宽带套餐", "5G套餐", "企业套餐", "家庭套餐", "学生套餐", "老年套餐"};
    private final String[] FEE_TYPES = {"通话费", "流量费", "短信费", "增值服务费"};
    private final String[] CHARGE_UNITS = {"分钟", "MB", "GB", "条"};
    private final String[] RULE_TYPES = {"阶梯计费", "固定费率", "包月计费", "按次计费"};
    private final String[] PROMO_TYPES = {"折扣优惠", "满减优惠", "赠送流量", "赠送通话"};
    private final String[] USAGE_TYPES = {"通话", "流量", "短信", "增值服务"};
    private final String[] USAGE_UNITS = {"分钟", "MB", "条", "次"};
    private final String[] RECHARGE_METHODS = {"支付宝", "微信支付", "银行卡", "现金", "自动扣费"};
    private final String[] RECHARGE_STATUSES = {"成功", "失败", "处理中"};
    private final String[] FILE_TYPES = {"pdf", "doc", "docx", "jpg", "png", "xlsx", "txt"};
    private final String[] OPERATION_TYPES = {"上传", "下载", "删除", "查看", "修改"};
    private final String[] OPERATION_STATUSES = {"成功", "失败"};
    private final String[] RESOURCE_TYPES = {"data", "voice", "sms", "broadband", "value_added"};

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("开始检查数据库表并生成模拟数据");
        System.out.println("========================================");

        Map<String, Integer> report = new LinkedHashMap<>();

        report.put("account", generateAccountData());
        report.put("admin", generateAdminData());
        report.put("user_number", generateUserNumberData());
        report.put("product", generateProductData());
        report.put("product_rate", generateProductRateData());
        report.put("promotion_rule", generatePromotionRuleData());
        report.put("user_number_product", generateUserNumberProductData());
        report.put("recharge", generateRechargeData());
        report.put("package_detail", generatePackageDetailData());
        report.put("usage_detail", generateUsageDetailData());
        report.put("kb_knowledge_base", generateKnowledgeBaseData());
        report.put("kb_document", generateKbDocumentData());
        report.put("file_operation_log", generateFileOperationLogData());

        System.out.println("\n========================================");
        System.out.println("数据生成报告");
        System.out.println("========================================");
        report.forEach((table, count) -> {
            System.out.println(String.format("%-25s: %d 条记录", table, count));
        });
        System.out.println("========================================");
        System.out.println("数据生成完成！");
        System.out.println("========================================");
    }

    private int generateAccountData() {
        if (accountRepository.count() > 0) {
            System.out.println("account 表已有数据，跳过生成");
            return (int) accountRepository.count();
        }

        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Account account = new Account();
            account.setAccountId("ACC" + String.format("%017d", i + 1));
            account.setRealName(REAL_NAMES[i % REAL_NAMES.length] + (i / REAL_NAMES.length + 1));
            account.setIdType(ID_TYPES[random.nextInt(ID_TYPES.length)]);
            account.setIdNumber(generateIdNumber());
            account.setTotalBalance(new BigDecimal(random.nextInt(10000) + 100).setScale(2, RoundingMode.HALF_UP));
            account.setAccountStatus(random.nextBoolean());
            Instant now = Instant.now();
            account.setCreatedAt(now.minus(random.nextInt(365), ChronoUnit.DAYS));
            account.setUpdatedAt(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            accounts.add(account);
        }
        accountRepository.saveAll(accounts);
        System.out.println("account 表: 生成 20 条记录");
        return 20;
    }

    private int generateAdminData() {
        if (adminRepository.count() > 0) {
            System.out.println("admin 表已有数据，跳过生成");
            return (int) adminRepository.count();
        }

        List<Admin> admins = new ArrayList<>();
        String[] usernames = {"admin", "manager", "supervisor", "operator1", "operator2"};
        String[] passwords = {"$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH", 
                              "$2a$10$O.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH",
                              "$2a$10$P.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH",
                              "$2a$10$Q.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH",
                              "$2a$10$R.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH"};
        
        for (int i = 0; i < usernames.length; i++) {
            Admin admin = new Admin();
            admin.setAdminId((long) (i + 1));
            admin.setUsername(usernames[i]);
            admin.setPassword(passwords[i]);
            admin.setAuthority((byte) (i == 0 ? 1 : (i == 1 ? 2 : 3)));
            admins.add(admin);
        }
        adminRepository.saveAll(admins);
        System.out.println("admin 表: 生成 5 条记录");
        return 5;
    }

    private int generateUserNumberData() {
        if (userNumberRepository.count() > 0) {
            System.out.println("user_number 表已有数据，跳过生成");
            return (int) userNumberRepository.count();
        }

        List<UserNumber> userNumbers = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UserNumber userNumber = new UserNumber();
            userNumber.setNumberId("NUM" + UUID.randomUUID().toString().substring(0, 28).replace("-", ""));
            userNumber.setNumberType((byte) random.nextInt(3));
            userNumber.setNumberValue(generatePhoneNumber());
            userNumber.setNumberStatus((byte) random.nextInt(3));
            Instant now = Instant.now();
            userNumber.setCreateTime(now.minus(random.nextInt(365), ChronoUnit.DAYS));
            userNumber.setUpdateTime(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            userNumbers.add(userNumber);
        }
        userNumberRepository.saveAll(userNumbers);
        System.out.println("user_number 表: 生成 30 条记录");
        return 30;
    }

    private int generateProductData() {
        if (productRepository.count() > 0) {
            System.out.println("product 表已有数据，跳过生成");
            return (int) productRepository.count();
        }

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Product product = new Product();
            product.setProductId((long) (i + 1));
            product.setProductName(PRODUCT_NAMES[i % PRODUCT_NAMES.length] + (i / PRODUCT_NAMES.length + 1));
            product.setProductType(random.nextBoolean());
            product.setBasePrice(new BigDecimal(random.nextInt(500) + 10).setScale(2));
            product.setEffectiveMode(random.nextBoolean());
            product.setValidityType(random.nextBoolean());
            product.setStatus(random.nextBoolean());
            Instant now = Instant.now();
            product.setCreateTime(now.minus(random.nextInt(365), ChronoUnit.DAYS));
            product.setUpdateTime(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            products.add(product);
        }
        productRepository.saveAll(products);
        System.out.println("product 表: 生成 15 条记录");
        return 15;
    }

    private int generateProductRateData() {
        if (productRateRepository.count() > 0) {
            System.out.println("product_rate 表已有数据，跳过生成");
            return (int) productRateRepository.count();
        }

        List<ProductRate> productRates = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            ProductRate productRate = new ProductRate();
            productRate.setRuleId("RULE" + UUID.randomUUID().toString().substring(0, 27).replace("-", ""));
            productRate.setProductId((long) (random.nextInt(15) + 1));
            productRate.setFeeType(FEE_TYPES[random.nextInt(FEE_TYPES.length)]);
            productRate.setChargeUnit(CHARGE_UNITS[random.nextInt(CHARGE_UNITS.length)]);
            productRate.setUnitPrice(BigDecimal.valueOf(random.nextDouble() * 10).setScale(2, RoundingMode.HALF_UP));
            productRate.setRuleType(RULE_TYPES[random.nextInt(RULE_TYPES.length)]);
            productRate.setConditionExpr("{\"min_value\":" + random.nextInt(100) + ",\"max_value\":" + (random.nextInt(900) + 100) + "}");
            Instant now = Instant.now();
            productRate.setEffectiveTime(now.minus(random.nextInt(180), ChronoUnit.DAYS));
            productRate.setExpireTime(now.plus(random.nextInt(365), ChronoUnit.DAYS));
            productRate.setIsActive((byte) (random.nextBoolean() ? 1 : 0));
            productRate.setCreatedAt(now.minus(random.nextInt(180), ChronoUnit.DAYS));
            productRate.setUpdatedAt(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            productRates.add(productRate);
        }
        productRateRepository.saveAll(productRates);
        System.out.println("product_rate 表: 生成 25 条记录");
        return 25;
    }

    private int generatePromotionRuleData() {
        if (promotionRuleRepository.count() > 0) {
            System.out.println("promotion_rule 表已有数据，跳过生成");
            return (int) promotionRuleRepository.count();
        }

        List<PromotionRule> promotionRules = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            PromotionRule promotionRule = new PromotionRule();
            promotionRule.setPromoId("PROMO" + UUID.randomUUID().toString().substring(0, 25).replace("-", ""));
            promotionRule.setProductId((long) (random.nextInt(15) + 1));
            promotionRule.setPromoType(PROMO_TYPES[random.nextInt(PROMO_TYPES.length)]);
            promotionRule.setConditionExpr("{\"min_amount\":" + random.nextInt(500) + ",\"discount_rate\":" + (random.nextDouble() * 0.5 + 0.1) + "}");
            promotionRule.setDiscountValue(BigDecimal.valueOf(random.nextDouble() * 100).setScale(2, RoundingMode.HALF_UP));
            Instant now = Instant.now();
            promotionRule.setStartTime(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            promotionRule.setEndTime(now.plus(random.nextInt(180), ChronoUnit.DAYS));
            promotionRule.setIsActive((byte) (random.nextBoolean() ? 1 : 0));
            promotionRule.setCreatedAt(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            promotionRule.setUpdatedAt(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            promotionRules.add(promotionRule);
        }
        promotionRuleRepository.saveAll(promotionRules);
        System.out.println("promotion_rule 表: 生成 20 条记录");
        return 20;
    }

    private int generateUserNumberProductData() {
        if (userNumberProductRepository.count() > 0) {
            System.out.println("user_number_product 表已有数据，跳过生成");
            return (int) userNumberProductRepository.count();
        }

        List<UserNumberProduct> userNumberProducts = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            UserNumberProduct userNumberProduct = new UserNumberProduct();
            userNumberProduct.setNumberId("NUM" + UUID.randomUUID().toString().substring(0, 28).replace("-", ""));
            userNumberProduct.setProductId(String.valueOf(random.nextInt(15) + 1));
            Instant now = Instant.now();
            userNumberProduct.setEffectiveTime(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            userNumberProduct.setExpireTime(now.plus(random.nextInt(365), ChronoUnit.DAYS));
            userNumberProduct.setStatus((byte) random.nextInt(3));
            userNumberProduct.setCreateTime(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            userNumberProduct.setUpdateTime(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            userNumberProducts.add(userNumberProduct);
        }
        userNumberProductRepository.saveAll(userNumberProducts);
        System.out.println("user_number_product 表: 生成 40 条记录");
        return 40;
    }

    private int generateRechargeData() {
        if (rechargeRepository.count() > 0) {
            System.out.println("recharge 表已有数据，跳过生成");
            return (int) rechargeRepository.count();
        }

        List<Recharge> recharges = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Recharge recharge = new Recharge();
            recharge.setPhone(generatePhoneNumber());
            recharge.setAmount(new BigDecimal(random.nextInt(1000) + 10).setScale(2, RoundingMode.HALF_UP));
            Instant now = Instant.now();
            recharge.setTime(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            recharge.setStatus(RECHARGE_STATUSES[random.nextInt(RECHARGE_STATUSES.length)]);
            recharge.setMethod(RECHARGE_METHODS[random.nextInt(RECHARGE_METHODS.length)]);
            recharge.setRemark(random.nextBoolean() ? "用户充值" + (i + 1) : null);
            recharge.setCreatedAt(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            recharge.setUpdatedAt(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            recharges.add(recharge);
        }
        rechargeRepository.saveAll(recharges);
        System.out.println("recharge 表: 生成 50 条记录");
        return 50;
    }

    private int generatePackageDetailData() {
        if (packageDetailRepository.count() > 0) {
            System.out.println("package_detail 表已有数据，跳过生成");
            return (int) packageDetailRepository.count();
        }

        System.out.println("package_detail 表: 跳过生成（CHECK约束问题）");
        return 0;
    }

    private int generateUsageDetailData() {
        if (usageDetailRepository.count() > 0) {
            System.out.println("usage_detail 表已有数据，跳过生成");
            return (int) usageDetailRepository.count();
        }

        List<UsageDetail> usageDetails = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UsageDetail usageDetail = new UsageDetail();
            usageDetail.setUsageId("USAGE" + UUID.randomUUID().toString().substring(0, 26).replace("-", ""));
            usageDetail.setUserNumber(generatePhoneNumber());
            usageDetail.setRuleId((long) (random.nextInt(25) + 1));
            usageDetail.setProductId((long) (random.nextInt(15) + 1));
            usageDetail.setUsageType(USAGE_TYPES[random.nextInt(USAGE_TYPES.length)]);
            usageDetail.setUsageValue(BigDecimal.valueOf(random.nextDouble() * 1000).setScale(2, RoundingMode.HALF_UP));
            usageDetail.setUsageUnit(USAGE_UNITS[random.nextInt(USAGE_UNITS.length)]);
            Instant now = Instant.now();
            usageDetail.setUsageStartTime(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            usageDetail.setUsageEndTime(now.minus(random.nextInt(89), ChronoUnit.DAYS));
            usageDetail.setStatus("已计费");
            usageDetail.setCreatedAt(now.minus(random.nextInt(90), ChronoUnit.DAYS));
            usageDetail.setUpdatedAt(now.minus(random.nextInt(30), ChronoUnit.DAYS));
            int regionIdx = random.nextInt(4);
            usageDetail.setResourceInfo("{\"region\":\"" + ("北京上海广州深圳".substring(regionIdx * 2, regionIdx * 2 + 2)) + "\"}");
            usageDetails.add(usageDetail);
        }
        usageDetailRepository.saveAll(usageDetails);
        System.out.println("usage_detail 表: 生成 50 条记录");
        return 50;
    }

    private int generateKbDocumentData() {
        if (kbDocumentRepository.count() > 0) {
            System.out.println("kb_document 表已有数据，跳过生成");
            return (int) kbDocumentRepository.count();
        }

        List<KbDocument> kbDocuments = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            KbDocument kbDocument = new KbDocument();
            kbDocument.setKbId((long) (random.nextInt(5) + 1));
            kbDocument.setUserId((long) (random.nextInt(20) + 1));
            kbDocument.setFileName("文件_" + (i + 1) + "." + FILE_TYPES[random.nextInt(FILE_TYPES.length)]);
            kbDocument.setStoragePath("/uploads/" + kbDocument.getFileName());
            kbDocument.setFileSize((long) (random.nextInt(10 * 1024 * 1024) + 1024));
            kbDocument.setFileType(FILE_TYPES[random.nextInt(FILE_TYPES.length)].toUpperCase());
            kbDocument.setChunkCount(random.nextInt(100));
            kbDocument.setStatus((byte) (random.nextBoolean() ? 1 : 0));
            kbDocument.setCurrentStep((byte) (random.nextInt(5) + 1));
            LocalDateTime createTime = LocalDateTime.now().minusDays(random.nextInt(90));
            kbDocument.setCreateTime(createTime);
            kbDocument.setUpdateTime(createTime.plusDays(random.nextInt(30)));
            kbDocuments.add(kbDocument);
        }
        kbDocumentRepository.saveAll(kbDocuments);
        System.out.println("kb_document 表: 生成 25 条记录");
        return 25;
    }

    private int generateKnowledgeBaseData() {
        if (knowledgeBaseRepository.count() > 0) {
            System.out.println("kb_knowledge_base 表已有数据，跳过生成");
            return (int) knowledgeBaseRepository.count();
        }

        List<KnowledgeBase> knowledgeBases = new ArrayList<>();
        String[] types = {"tech", "business", "policy"};
        String[] departments = {"技术部", "市场部", "人事部", "财务部", "运营部"};
        
        for (int i = 0; i < 5; i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName("知识库_" + (i + 1));
            kb.setDescription("这是知识库" + (i + 1) + "的描述信息");
            kb.setType(types[random.nextInt(types.length)]);
            kb.setOwner(REAL_NAMES[random.nextInt(REAL_NAMES.length)]);
            kb.setDepartment(departments[random.nextInt(departments.length)]);
            kb.setVectorDim(List.of(768, 1024, 1536, 2048).get(random.nextInt(4)));
            kb.setDocCount(random.nextInt(50));
            kb.setStatus((byte) (random.nextBoolean() ? 1 : 0));
            kb.setDeleted((byte) 0);
            LocalDateTime createTime = LocalDateTime.now().minusDays(random.nextInt(180));
            kb.setCreateTime(createTime);
            kb.setUpdateTime(createTime.plusDays(random.nextInt(60)));
            knowledgeBases.add(kb);
        }
        
        knowledgeBaseRepository.saveAll(knowledgeBases);
        System.out.println("kb_knowledge_base 表: 生成 5 条记录");
        return 5;
    }

    private int generateFileOperationLogData() {
        if (fileOperationLogRepository.count() > 0) {
            System.out.println("file_operation_log 表已有数据，跳过生成");
            return (int) fileOperationLogRepository.count();
        }

        List<FileOperationLog> fileOperationLogs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            FileOperationLog fileOperationLog = new FileOperationLog();
            fileOperationLog.setUserId((long) (random.nextInt(20) + 1));
            fileOperationLog.setDocId((long) (random.nextInt(25) + 1));
            fileOperationLog.setOperationType(OPERATION_TYPES[random.nextInt(OPERATION_TYPES.length)]);
            fileOperationLog.setOperationTime(LocalDateTime.now().minusDays(random.nextInt(90)));
            fileOperationLog.setIpAddress("192.168." + random.nextInt(256) + "." + random.nextInt(256));
            fileOperationLog.setStatus(OPERATION_STATUSES[random.nextInt(OPERATION_STATUSES.length)]);
            if (fileOperationLog.getStatus().equals("失败")) {
                int errorIdx = random.nextInt(4);
                fileOperationLog.setErrorMessage("操作失败: " + ("权限不足文件不存在网络错误".substring(errorIdx * 4, errorIdx * 4 + 4)));
            }
            fileOperationLogs.add(fileOperationLog);
        }
        fileOperationLogRepository.saveAll(fileOperationLogs);
        System.out.println("file_operation_log 表: 生成 40 条记录");
        return 40;
    }

    private String generatePhoneNumber() {
        return PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)] + 
               String.format("%08d", random.nextInt(100000000));
    }

    private String generateIdNumber() {
        return String.format("%d", 110000000000000L + Math.abs(random.nextLong() % 900000000000000L));
    }
}
