package backend.exceptions;

public class UnexpectedErrorException extends Throwable {

    public UnexpectedErrorException(){
        super("Unexpected Error occured");
    }

    public UnexpectedErrorException(String message) {
        super(message);
    }
}
