package com.example.dicom_converter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DicomConverterApplication implements CommandLineRunner {

    // @Autowired: Spring이 관리하는 DicomConversionService 객체를 여기에 자동으로 연결
    private final DicomConversionService conversionService;

    public DicomConverterApplication(DicomConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public static void main(String[] args) {
        // Spring Boot 애플리케이션 시작
        SpringApplication.run(DicomConverterApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 애플리케이션이 시작되면 이 코드가 실행됨
        conversionService.startConversion();
    }
}