package com.example.dicom_converter;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.regex.Pattern;

@Service
public class DicomConversionService {

    // 1. application.properties에서 설정값 자동 주입
    @Value("${dicom.source-dir}")
    private String sourceDir;

    @Value("${dicom.dest-dir}")
    private String destDir;

    // Python의 os.walk와 유사한 기능 수행
    public void startConversion() throws Exception {
        System.out.println("스캔 시작 (네트워크): " + sourceDir);
        System.out.println("저장 위치 (루트): " + destDir);

        Path baseDirPath = Paths.get(sourceDir);

        // Java 8의 Files.walk (Python의 os.walk와 동일)
        Files.walk(baseDirPath)
                // 2. 'MR' 이름을 가진 폴더만 필터링
                .filter(Files::isDirectory)
                .filter(dir -> dir.getFileName().toString().equals("MR"))
                .forEach(this::processMrFolder); // 3. 각 'MR' 폴더에 대해 processMrFolder 실행

        System.out.println("--- 작업 완료 ---");
    }

    /**
     * 'MR' 폴더 하나를 처리하는 메서드 (Python 코드의 핵심 루프)
     */
    private void processMrFolder(Path mrDirPath) {
        try {
            // 4. 경로 정보 추출 (Python의 os.path.basename/dirname)
            Path studyDateDir = mrDirPath.getParent();
            Path patientDir = studyDateDir.getParent();
            String patientId = patientDir.getFileName().toString();
            String studyDate = studyDateDir.getFileName().toString();

            System.out.println(String.format("\n--- ['MR' 폴더 발견] 환자: %s, 날짜: %s ---", patientId, studyDate));

            // 5. 동적 저장 경로 생성 (Python의 os.path.join)
            Path relativeMrDir = Paths.get(sourceDir).relativize(mrDirPath); // 00456409\20220104\MR
            Path patientAndDatePart = relativeMrDir.getParent(); // 00456409\20220104
            Path currentDestDir = Paths.get(destDir, patientAndDatePart.toString(), "MR");



            // 6. Python의 series_map과 동일한 구조
            // Key: SeriesInstanceUID, Value: 정렬을 위한 슬라이스 정보 리스트
            Map<String, List<DicomSliceInfo>> seriesMap = new HashMap<>();

            // 7. 1차 스캔: 메타데이터만 읽고 정렬을 위해 맵에 저장
            Files.list(mrDirPath)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        try (DicomInputStream dis = new DicomInputStream(filePath.toFile())) {
                            Attributes attrs = dis.readDataset(-1, -1);

                            String seriesDesc = attrs.getString(Tag.SeriesDescription, "").strip().toLowerCase();
                            if (seriesDesc.startsWith("t1") && seriesDesc.endsWith("ax")) {
                                String seriesUID = attrs.getString(Tag.SeriesInstanceUID, "UnknownUID");
                                int instanceNum = attrs.getInt(Tag.InstanceNumber, 0);

                                seriesMap
                                        .computeIfAbsent(seriesUID, k -> new ArrayList<>())
                                        .add(new DicomSliceInfo(instanceNum, filePath, seriesDesc));
                            }
                        } catch (Exception e) {
                            System.err.println("  [파일 읽기 오류] " + filePath.getFileName() + " : " + e.getMessage());
                            e.printStackTrace(); // <-- (필수!)
                        }
                    });

            if (seriesMap.isEmpty()) {
                System.out.println("  ... 'T1...AX' 시리즈를 찾지 못했습니다.");
                return;
            }

            try {
                Files.createDirectories(currentDestDir); // os.makedirs
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }


            // 8. 2차 스캔: 맵을 순회하며 정렬 및 PNG 변환
            Pattern safeNamePattern = Pattern.compile("[\\W_]+");

            for (Map.Entry<String, List<DicomSliceInfo>> entry : seriesMap.entrySet()) {
                List<DicomSliceInfo> slices = entry.getValue();

                // 9. InstanceNumber 기준으로 정렬 (Python의 sorted(..., key=...))
                slices.sort(Comparator.comparingInt(DicomSliceInfo::getInstanceNum));

                if (slices.isEmpty()) continue;

                String origDesc = slices.get(0).getSeriesDesc();
                String safeSeriesName = safeNamePattern.matcher(origDesc).replaceAll("_").strip();

                System.out.println(String.format("    [시리즈 변환] %s (총 %d장)", safeSeriesName, slices.size()));

                for (DicomSliceInfo slice : slices) {
                    try {
                        // 10. dcm4che-imageio가 설치되어 있으면 ImageIO.read로 바로 변환 가능
                        BufferedImage image = ImageIO.read(slice.getFilePath().toFile());

                        // (참고: Python의 Min-Max 정규화 로직은 여기서 제외했으나,
                        //  BufferedImage 픽셀에 접근하여 동일하게 구현 가능합니다)

                        String pngFilename = String.format("%s_%s_%s_%04d.png",
                                patientId, studyDate, safeSeriesName, slice.getInstanceNum());

                        Path destPath = currentDestDir.resolve(pngFilename);
                        ImageIO.write(image, "png", destPath.toFile());

                    } catch (Exception e) {
                        System.err.println("    [PNG 변환 오류] (슬라이스 " + slice.getInstanceNum() + ")");
                        e.printStackTrace(); // <-- (필수!) 진짜 오류 내용을 출력합니다.
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[오류] MR 폴더 처리 실패: " + mrDirPath);
            e.printStackTrace();
        }
    }

    // 11. 정렬을 위한 간단한 데이터 클래스 (Python의 튜플 역할)
    private static class DicomSliceInfo {
        private final int instanceNum;
        private final Path filePath;
        private final String seriesDesc;

        public DicomSliceInfo(int instanceNum, Path filePath, String seriesDesc) {
            this.instanceNum = instanceNum;
            this.filePath = filePath;
            this.seriesDesc = seriesDesc;
        }

        public int getInstanceNum() { return instanceNum; }
        public Path getFilePath() { return filePath; }
        public String getSeriesDesc() { return seriesDesc; }
    }
}