package com.nordstrom.finance.dataintegration.facade.schema.restaurant;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.STORE_DEFAULT;
import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionCardType;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionMenuLineItem;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionSourceExperience;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionSupplementaryIdentifiers;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionTender;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionTenderType;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionType;
import com.nordstrom.event.rosettastone.Employee;
import com.nordstrom.event.rosettastone.EmployeeIdType;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.MoneyV2;
import com.nordstrom.standard.TimezoneId;
import java.util.List;
import java.util.function.UnaryOperator;

public class FinancialRestaurantTransactionBuilderFacade {
  private FinancialRestaurantTransactionBuilderFacade() {}

  public static FinancialRestaurantTransaction build(
      UnaryOperator<FinancialRestaurantTransaction.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static FinancialRestaurantTransaction.Builder getDefaultBuilder() {
    MoneyV2 defaultMoney = MoneyBuilderFacade.buildDefault();

    return FinancialRestaurantTransaction.newBuilder()
        .setIsReversed(false)
        .setFinancialRestaurantTransactionRecordId("SdmId-1")
        .setTransactionType(FinancialRestaurantTransactionType.SALE)
        .setBusinessDateTime(TESTING_INSTANT)
        .setSupplementaryIdentifiers(
            FinancialRestaurantTransactionSupplementaryIdentifiers.newBuilder()
                .setCustomerTransactionId("Cust-1")
                .setRestaurantOrderId("Rest-Order_1")
                .build())
        .setCreatedTime(TESTING_INSTANT)
        .setTransactionTime(TESTING_INSTANT)
        .setRestaurantName("Rest-1")
        .setRingingDateTime(TESTING_INSTANT)
        .setEmployee(
            Employee.newBuilder().setId("Id-1").setIdType(EmployeeIdType.EMPLOYEE_EMAIL).build())
        .setSourceExperience(FinancialRestaurantTransactionSourceExperience.IN_STORE)
        .setAuditActivityDetail(AuditActivityDetailBuilderFacade.buildDefault())
        .setValidationResult(ValidationResultBuilderFacade.buildDefault())
        .setTaxTotal(MoneyBuilderFacade.build(1, 0))
        .setTotalAmount(defaultMoney)
        .setVersionNumber("1")
        .setTipTotal(MoneyBuilderFacade.build(0, 0))
        .setStoreLocationId(STORE_DEFAULT)
        .setStoreTimezone(TimezoneId.US_PACIFIC_TIME)
        .setTotalAmountExcludingTipsAndTaxes(defaultMoney)
        .setIsReversed(false)
        .setPosDeviceId("POS-Device-1")
        .setStoreLocationId(STORE_DEFAULT)
        .setMenuItems(MenuItemBuilderFacade.buildDefaultList(3))
        .setTenders(List.of(TenderDetailBuilderFacade.buildDefault()));
  }

  public static FinancialRestaurantTransaction saleWithSingleItemAndSingleTender() {
    List<FinancialRestaurantTransactionMenuLineItem> menuItems =
        getSingleFinancialRestaurantTransactionMenuLineItems();
    List<FinancialRestaurantTransactionTender> tenders = getSingleTenderList();
    return getDefaultBuilder()
        .setFinancialRestaurantTransactionRecordId("SdmId-1")
        .setMenuItems(menuItems)
        .setTenders(tenders)
        .build();
  }

  public static FinancialRestaurantTransaction reversedSaleWithSingleItemAndSingleTender() {
    List<FinancialRestaurantTransactionMenuLineItem> menuItems =
        getSingleFinancialRestaurantTransactionMenuLineItems();
    List<FinancialRestaurantTransactionTender> tenders = getSingleTenderList();
    return getDefaultBuilder()
        .setFinancialRestaurantTransactionRecordId("SdmId-2")
        .setMenuItems(menuItems)
        .setTenders(tenders)
        .setIsReversed(true)
        .build();
  }

  public static FinancialRestaurantTransaction returnWithSingleItemAndSingleTender() {
    List<FinancialRestaurantTransactionMenuLineItem> menuItems =
        getSingleFinancialRestaurantTransactionMenuLineItems();
    List<FinancialRestaurantTransactionTender> tenders = getSingleTenderList();

    return getDefaultBuilder()
        .setTransactionType(FinancialRestaurantTransactionType.RETURN)
        .setFinancialRestaurantTransactionRecordId("SdmId-5")
        .setMenuItems(menuItems)
        .setTenders(tenders)
        .build();
  }

  public static FinancialRestaurantTransaction reversedReturnWithSingleItemAndSingleTender() {

    List<FinancialRestaurantTransactionMenuLineItem> menuItems =
        getSingleFinancialRestaurantTransactionMenuLineItems();
    List<FinancialRestaurantTransactionTender> tenders = getSingleTenderList();
    return getDefaultBuilder()
        .setTransactionType(FinancialRestaurantTransactionType.RETURN)
        .setFinancialRestaurantTransactionRecordId("SdmId-7")
        .setMenuItems(menuItems)
        .setTenders(tenders)
        .setIsReversed(true)
        .build();
  }

  private static List<FinancialRestaurantTransactionTender> getSingleTenderList() {
    return List.of(
        TenderDetailBuilderFacade.build(
            TenderDetailBuilderFacade.buildWithTenderTypesAndAmount(
                FinancialRestaurantTransactionTenderType.CREDIT_CARD,
                FinancialRestaurantTransactionCardType.AMERICAN_EXPRESS,
                null,
                MoneyBuilderFacade.build(110, 0))));
  }

  private static List<FinancialRestaurantTransactionMenuLineItem>
      getSingleFinancialRestaurantTransactionMenuLineItems() {
    return List.of(
        MenuItemBuilderFacade.build(
            MenuItemBuilderFacade.withIdAmountsDeptAndClass(
                "MI-" + 0,
                MoneyBuilderFacade.build(100, 0),
                MoneyBuilderFacade.build(10, 0),
                MoneyBuilderFacade.build(0, 0),
                "0005",
                "0001")));
  }
}
