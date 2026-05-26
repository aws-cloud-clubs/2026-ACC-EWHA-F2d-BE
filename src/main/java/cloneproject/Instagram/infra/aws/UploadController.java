package cloneproject.Instagram.infra.aws;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import cloneproject.Instagram.global.result.ResultResponse;
import cloneproject.Instagram.infra.aws.S3Uploader.PresignedUrlResponse;

import static cloneproject.Instagram.global.result.ResultCode.GET_PRESIGNED_URL_SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class UploadController {

    private final S3Uploader s3Uploader;

    // 프리사인 PUT URL 발급
    // GET /upload/presigned-url?dir=post&filename=photo.jpg&contentType=image/jpeg
    @GetMapping("/presigned-url")
    public ResponseEntity<ResultResponse> getPresignedUrl(
            @RequestParam String dir,
            @RequestParam String filename,
            @RequestParam String contentType) {

        PresignedUrlResponse response = s3Uploader.generatePresignedPutUrl(dir, filename, contentType);
        return ResponseEntity.ok(ResultResponse.of(GET_PRESIGNED_URL_SUCCESS, response));
    }
}