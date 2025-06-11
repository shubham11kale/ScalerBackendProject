package capstoneproject.ProductService.paymentservice;

import capstoneproject.ProductService.dtos.PaymentClientDto;
import capstoneproject.ProductService.exceptions.PaymentClientException;

public interface PaymentServiceClient {
    PaymentClientDto createPaymentOrder(String invoiceNumber, String currency, Double amount) throws PaymentClientException;
    PaymentClientDto getPaymentStatus(String paymentOrderId) throws PaymentClientException;
    PaymentClientDto processRefund(String paymentOrderId) throws PaymentClientException;
}
