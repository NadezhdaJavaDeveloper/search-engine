package searchengine.exaptions;

public class InconsistencyWithConfigurationFile extends RuntimeException{


    //Данная страница находится за пределами сайтов,указанных в конфигурационном файле

    public InconsistencyWithConfigurationFile(String error) {
        super(error);
    }
}