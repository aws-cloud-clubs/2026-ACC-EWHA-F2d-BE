package cloneproject.Instagram.infra.aws;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import cloneproject.Instagram.global.vo.Image;
import cloneproject.Instagram.global.vo.ImageType;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3Uploader {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    // 프리사인 PUT URL 발급 (프론트가 S3에 직접 업로드할 때 사용)
    public PresignedUrlResponse generatePresignedPutUrl(String dirName, String originalFilename, String contentType) {
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalFilename);
        String baseName = extractBaseName(originalFilename);
        String s3Key = dirName + "/" + uuid + "_" + baseName + "." + extension;

        Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000); // 10분

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);
        request.addRequestParameter("Content-Type", contentType);

        String presignedUrl = amazonS3Client.generatePresignedUrl(request).toString();
        String cloudfrontUrl = "https://" + cloudfrontDomain + "/" + s3Key;

        return new PresignedUrlResponse(presignedUrl, s3Key, cloudfrontUrl);
    }

    // S3 키로 CloudFront URL 생성 (DB 저장용)
    public String buildCloudfrontUrl(String s3Key) {
        return "https://" + cloudfrontDomain + "/" + s3Key;
    }

    // S3 키로 Image VO 생성 (DB 저장용)
    public Image buildImage(String s3Key) {
        String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        String[] parts = filename.split("_", 2);
        String uuid = parts[0];
        String nameWithExt = parts.length > 1 ? parts[1] : filename;
        String extension = extractExtension(nameWithExt).toUpperCase();
        String baseName = extractBaseName(nameWithExt);
        String cloudfrontUrl = buildCloudfrontUrl(s3Key);

        return Image.builder()
                .imageUUID(uuid)
                .imageName(baseName)
                .imageType(ImageType.valueOf(extension))
                .imageUrl(cloudfrontUrl)
                .build();
    }

    // MultipartFile 직접 업로드 (기존 방식 호환용)
    public Image uploadImage(MultipartFile file, String dirName) {
        try {
            String originalFilename = file.getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String extension = extractExtension(originalFilename);
            String baseName = extractBaseName(originalFilename);
            String s3Key = dirName + "/" + uuid + "_" + baseName + "." + extension;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3Client.putObject(bucket, s3Key, file.getInputStream(), metadata);

            return buildImage(s3Key);
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    // S3 객체 삭제
    public void deleteImage(Image image, String dirName) {
        if ("base-UUID".equals(image.getImageUUID())) {
            return;
        }
        String s3Key = dirName + "/" + image.getImageUUID() + "_"
                + image.getImageName() + "." + image.getImageType().toString();
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, s3Key));
    }

    private String extractExtension(String filename) {
        int dotIdx = filename.lastIndexOf(".");
        return dotIdx >= 0 ? filename.substring(dotIdx + 1) : "";
    }

    private String extractBaseName(String filename) {
        int dotIdx = filename.lastIndexOf(".");
        return dotIdx >= 0 ? filename.substring(0, dotIdx) : filename;
    }

    // 프리사인 URL 발급 응답 DTO
    public static class PresignedUrlResponse {
        private final String presignedUrl;
        private final String s3Key;
        private final String cloudfrontUrl;

        public PresignedUrlResponse(String presignedUrl, String s3Key, String cloudfrontUrl) {
            this.presignedUrl = presignedUrl;
            this.s3Key = s3Key;
            this.cloudfrontUrl = cloudfrontUrl;
        }

        public String getPresignedUrl() { return presignedUrl; }
        public String getS3Key() { return s3Key; }
        public String getCloudfrontUrl() { return cloudfrontUrl; }
    }
}