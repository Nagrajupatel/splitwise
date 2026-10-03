package com.splitwise.web;

import com.splitwise.service.OcrService;
import com.splitwise.web.Dtos.OcrResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {
    private final OcrService ocr;

    @PostMapping("/scan")
    public OcrResult scan(@RequestParam("file") MultipartFile file) {
        return ocr.scan(file);
    }
}
