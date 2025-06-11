package capstoneproject.PaymentService.exceptions;

public class InvalidRefundException extends Exception{
    public InvalidRefundException(String message) {
        super(message);
    }
}
