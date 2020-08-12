package backend.exceptions;

public class InvalidFileNameException extends Exception {

    private static final String defaultError = "Dateiname ungültig";

    public InvalidFileNameException(String s) {
        super(s);
    }

    public InvalidFileNameException(){
        super(defaultError);
    }
}
