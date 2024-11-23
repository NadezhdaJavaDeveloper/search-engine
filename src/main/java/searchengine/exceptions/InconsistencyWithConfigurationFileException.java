package searchengine.exceptions;

public class InconsistencyWithConfigurationFileException extends RuntimeException{


    //Данная страница находится за пределами сайтов,указанных в конфигурационном файле

    public InconsistencyWithConfigurationFileException(String error) {
        super(error);
    }
}