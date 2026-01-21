package com.nordstrom.finance.dataintegration.mapper;

import static com.nordstrom.finance.dataintegration.common.util.StringFormatUtility.toFourDigitFormat;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_LINE_TYPE_ITEM;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_LINE_TYPE_TENDER;
import static com.nordstrom.finance.dataintegration.utility.DateUtility.toLocalDate;
import static com.nordstrom.finance.dataintegration.utility.MoneyUtility.getAmount;

import com.nordstrom.customer.object.operational.*;
import com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants;
import com.nordstrom.finance.dataintegration.database.entity.RestaurantTransactionLine;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.database.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.exception.*;
import com.nordstrom.finance.dataintegration.utility.DateUtility;
import com.nordstrom.finance.dataintegration.utility.MoneyUtility;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialRestaurantTransactionMapper {

  /** Maps a FinancialRestaurantTransaction to a Transaction entity. */
  public Transaction mapRestaurantSchemaToTransactionEntity(
      FinancialRestaurantTransaction restaurantTransaction) {
    if (restaurantTransaction == null) {
      log.warn("FinancialRestaurantTransaction is null, returning null Transaction");
      return null;
    }

    try {
      log.debug(
          "Mapping FinancialRestaurantTransaction [{}] to Transaction",
          restaurantTransaction.getFinancialRestaurantTransactionRecordId());

      Transaction transactionEntity =
          Transaction.builder()
              .sourceReferenceTransactionId(
                  restaurantTransaction.getFinancialRestaurantTransactionRecordId().toString())
              .sourceReferenceSystemType(
                  TransactionMappingConstants.SOURCE_REFERENCE_SYSTEM_TYPE_SDM)
              .sourceReferenceType(TransactionMappingConstants.SOURCE_REFERENCE_TYPE_RESTAURANT)
              .transactionDate(toLocalDate(restaurantTransaction.getTransactionTime()))
              .sourceProcessedDate(toLocalDate(restaurantTransaction.getCreatedTime()))
              .businessDate(
                  DateUtility.updateTimeZoneAndGetDate(
                      restaurantTransaction.getStoreTimezone(),
                      restaurantTransaction.getBusinessDateTime()))
              .transactionType(restaurantTransaction.getTransactionType().name())
              .transactionReversalCode(restaurantTransaction.getIsReversed() ? "Y" : "N")
              .build();
      transactionEntity.setTransactionLines(mapTransactionLines(restaurantTransaction));
      log.debug("Successfully mapped FinancialRestaurantTransaction to Transaction");
      return transactionEntity;
    } catch (Exception e) {
      log.error("Error mapping FinancialRestaurantTransaction to Transaction", e);
      throw new ObjectMappingException(
          "Failed to map FinancialRestaurantTransaction to Transaction", e);
    }
  }

  /**
   * Maps menu items and tenders from a FinancialRestaurantTransaction to a list of TransactionLine
   * entities.
   */
  private Set<TransactionLine> mapTransactionLines(
      FinancialRestaurantTransaction restaurantTransaction) {

    try {
      Set<TransactionLine> transactionLines = new HashSet<>();

      // ===== Menu Items =====
      List<FinancialRestaurantTransactionMenuLineItem> menuItems =
          restaurantTransaction.getMenuItems();
      if (menuItems != null) {
        for (var item : menuItems) {
          transactionLines.add(buildItemTransactionLine(restaurantTransaction, item));
        }
      }

      // ===== Tenders =====
      List<FinancialRestaurantTransactionTender> tenders = restaurantTransaction.getTenders();
      if (tenders != null) {
        for (var tenderDetail : tenders) {
          transactionLines.add(buildTenderTransactionLine(restaurantTransaction, tenderDetail));
        }
      }

      if (restaurantTransaction.getRestaurantDeliveryPartner().isPresent()) {
        transactionLines.add(buildRestaurantDeliveryPartnerTransactionLine(restaurantTransaction));
      }

      if (restaurantTransaction.getRestaurantLoyaltyBenefitType().isPresent()) {
        transactionLines.add(buildRestaurantBenefitTransactionLine(restaurantTransaction));
      }

      log.debug("Mapped {} TransactionLine(s)", transactionLines.size());
      return transactionLines;
    } catch (Exception e) {
      log.error("Error mapping FinancialRestaurantTransaction to TransactionLines", e);
      throw new ObjectMappingException(
          "Failed to map FinancialRestaurantTransaction to TransactionLines", e);
    }
  }

  private TransactionLine buildItemTransactionLine(
      FinancialRestaurantTransaction restaurantTransaction,
      FinancialRestaurantTransactionMenuLineItem menuLineItem) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(menuLineItem.getTransactionLineId().toString())
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_ITEM)
            .ringingStore(String.valueOf(restaurantTransaction.getStoreLocationId()))
            .storeOfIntent(null)
            .build();
    transactionLine.setRestaurantTransactionLine(buildRestaurantLineForMenuItem(menuLineItem));
    return transactionLine;
  }

  private TransactionLine buildTenderTransactionLine(
      FinancialRestaurantTransaction restaurantTransaction,
      FinancialRestaurantTransactionTender tenderDetail) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(tenderDetail.getId().toString())
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
            .ringingStore(String.valueOf(restaurantTransaction.getStoreLocationId()))
            .build();
    transactionLine.setRestaurantTransactionLine(buildRestaurantLineForTender(tenderDetail));
    return transactionLine;
  }

  private TransactionLine buildRestaurantDeliveryPartnerTransactionLine(
      FinancialRestaurantTransaction restaurantTransaction) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(
                restaurantTransaction.getFinancialRestaurantTransactionRecordId().toString())
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
            .ringingStore(String.valueOf(restaurantTransaction.getStoreLocationId()))
            .storeOfIntent(null)
            .build();
    transactionLine.setRestaurantTransactionLine(
        buildRestaurantLineForDeliveryPartner(restaurantTransaction));
    return transactionLine;
  }

  private TransactionLine buildRestaurantBenefitTransactionLine(
      FinancialRestaurantTransaction restaurantTransaction) {
    TransactionLine transactionLine =
        TransactionLine.builder()
            .sourceReferenceLineId(
                restaurantTransaction.getFinancialRestaurantTransactionRecordId().toString())
            .sourceReferenceLineType(SOURCE_REFERENCE_LINE_TYPE_TENDER)
            .ringingStore(String.valueOf(restaurantTransaction.getStoreLocationId()))
            .storeOfIntent(null)
            .build();
    transactionLine.setRestaurantTransactionLine(
        buildRestaurantLineForBenefit(restaurantTransaction));
    return transactionLine;
  }

  private RestaurantTransactionLine buildRestaurantLineForMenuItem(
      FinancialRestaurantTransactionMenuLineItem item) {
    return RestaurantTransactionLine.builder()
        .lineItemAmount(getAmount(item.getPriceAfterPromosBeforeEmployeeDiscount()))
        .taxAmount(getAmount(item.getTotalTax()))
        .employeeDiscountAmount(
            item.getDiscounts().stream()
                .filter(
                    d ->
                        d.getDiscountType()
                            .equals(FinancialRestaurantTransactionDiscountType.EMPLOYEE_DISCOUNT))
                .map(FinancialRestaurantTransactionDiscount::getDiscountAmount)
                .findFirst()
                .map(MoneyUtility::getAmount)
                .orElse(BigDecimal.ZERO))
        .departmentId(
            item.getItemDepartmentAndClass() != null
                    && item.getItemDepartmentAndClass().getDepartmentNumber() != null
                ? toFourDigitFormat(
                    item.getItemDepartmentAndClass().getDepartmentNumber().toString())
                : null)
        .classId(
            item.getItemDepartmentAndClass() != null
                    && item.getItemDepartmentAndClass().getClassNumber() != null
                ? toFourDigitFormat(item.getItemDepartmentAndClass().getClassNumber().toString())
                : null)
        .build();
  }

  private RestaurantTransactionLine buildRestaurantLineForTender(
      FinancialRestaurantTransactionTender tender) {

    return RestaurantTransactionLine.builder()
        .tenderType(tender.getTenderType().name())
        .tenderCardTypeCode(tender.getCardType().map(Enum::name).orElse(null))
        .tenderCardSubTypeCode(tender.getCardSubType().map(Enum::name).orElse(null))
        .tenderAmount(
            getAmount(tender.getTotal()) == null
                ? BigDecimal.ZERO
                : getAmount(tender.getTotal())
                    .add(
                        getAmount(tender.getTipTotal()) == null
                            ? BigDecimal.ZERO
                            : getAmount(tender.getTipTotal())))
        .restaurantTipAmount(getAmount(tender.getTipTotal()))
        .build();
  }

  private RestaurantTransactionLine buildRestaurantLineForDeliveryPartner(
      FinancialRestaurantTransaction transaction) {
    return RestaurantTransactionLine.builder()
        .tenderAmount(getAmount(transaction.getTotalAmount()))
        .restaurantDeliveryPartner(
            transaction.getRestaurantDeliveryPartner().map(Enum::name).orElse(null))
        .build();
  }

  private RestaurantTransactionLine buildRestaurantLineForBenefit(
      FinancialRestaurantTransaction transaction) {
    return RestaurantTransactionLine.builder()
        .tenderAmount(getAmount(transaction.getTotalAmount()))
        .restaurantTipAmount(getAmount(transaction.getTipTotal()))
        .restaurantLoyaltyBenefitType(
            transaction.getRestaurantLoyaltyBenefitType().map(Enum::name).orElse(null))
        .build();
  }
}
