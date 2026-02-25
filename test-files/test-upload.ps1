# 文件上传接口测试脚本

# 1. 测试登录获取Token
Write-Host "=== 测试1: 登录获取Token ===" -ForegroundColor Green
$loginBody = @{
    username = "admin"
    password = "1234567"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    Write-Host "登录成功!" -ForegroundColor Green
    Write-Host "响应: $($loginResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
    
    $token = $loginResponse.data.token
    Write-Host "Token: $token" -ForegroundColor Yellow
    
    $headers = @{
        "Authorization" = "Bearer $token"
    }
} catch {
    Write-Host "登录失败: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. 测试单文件上传 - TXT文件
Write-Host "=== 测试2: 单文件上传 - TXT文件 ===" -ForegroundColor Green
$testFilePath = "test-files/test.txt"
if (Test-Path $testFilePath) {
    try {
        $fileContent = [System.IO.File]::ReadAllBytes($testFilePath)
        $fileBytes = [System.IO.MemoryStream]::new($fileContent)
        $fileStream = [System.IO.StreamReader]::new($fileBytes)
        
        $multipartContent = [System.Net.Http.MultipartFormDataContent]::new()
        $fileStreamContent = [System.Net.Http.StreamContent]::new($fileBytes)
        $multipartContent.Add($fileStreamContent, "file", "test.txt")
        
        $uploadResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/upload" -Method POST -Headers $headers -Body $multipartContent
        Write-Host "TXT文件上传成功!" -ForegroundColor Green
        Write-Host "响应: $($uploadResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
    } catch {
        Write-Host "TXT文件上传失败: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "测试文件不存在: $testFilePath" -ForegroundColor Yellow
}

Write-Host ""

# 3. 测试单文件上传 - PDF文件
Write-Host "=== 测试3: 单文件上传 - PDF文件 ===" -ForegroundColor Green
$testFilePath = "test-files/test.pdf"
if (Test-Path $testFilePath) {
    try {
        $fileContent = [System.IO.File]::ReadAllBytes($testFilePath)
        $fileBytes = [System.IO.MemoryStream]::new($fileContent)
        
        $multipartContent = [System.Net.Http.MultipartFormDataContent]::new()
        $fileStreamContent = [System.Net.Http.StreamContent]::new($fileBytes)
        $multipartContent.Add($fileStreamContent, "file", "test.pdf")
        
        $uploadResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/upload" -Method POST -Headers $headers -Body $multipartContent
        Write-Host "PDF文件上传成功!" -ForegroundColor Green
        Write-Host "响应: $($uploadResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
    } catch {
        Write-Host "PDF文件上传失败: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "测试文件不存在: $testFilePath" -ForegroundColor Yellow
}

Write-Host ""

# 4. 测试单文件上传 - DOCX文件
Write-Host "=== 测试4: 单文件上传 - DOCX文件 ===" -ForegroundColor Green
$testFilePath = "test-files/test.docx"
if (Test-Path $testFilePath) {
    try {
        $fileContent = [System.IO.File]::ReadAllBytes($testFilePath)
        $fileBytes = [System.IO.MemoryStream]::new($fileContent)
        
        $multipartContent = [System.Net.Http.MultipartFormDataContent]::new()
        $fileStreamContent = [System.Net.Http.StreamContent]::new($fileBytes)
        $multipartContent.Add($fileStreamContent, "file", "test.docx")
        
        $uploadResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/upload" -Method POST -Headers $headers -Body $multipartContent
        Write-Host "DOCX文件上传成功!" -ForegroundColor Green
        Write-Host "响应: $($uploadResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
    } catch {
        Write-Host "DOCX文件上传失败: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "测试文件不存在: $testFilePath" -ForegroundColor Yellow
}

Write-Host ""

# 5. 测试获取文件列表
Write-Host "=== 测试5: 获取文件列表 ===" -ForegroundColor Green
try {
    $listResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/list" -Method GET -Headers $headers
    Write-Host "获取文件列表成功!" -ForegroundColor Green
    Write-Host "响应: $($listResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
} catch {
    Write-Host "获取文件列表失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 6. 测试不支持的文件类型
Write-Host "=== 测试6: 不支持的文件类型 (exe) ===" -ForegroundColor Green
$testFilePath = "test-files/test.exe"
"test exe file content" | Out-File -FilePath $testFilePath -Encoding utf8
if (Test-Path $testFilePath) {
    try {
        $fileContent = [System.IO.File]::ReadAllBytes($testFilePath)
        $fileBytes = [System.IO.MemoryStream]::new($fileContent)
        
        $multipartContent = [System.Net.Http.MultipartFormDataContent]::new()
        $fileStreamContent = [System.Net.Http.StreamContent]::new($fileBytes)
        $multipartContent.Add($fileStreamContent, "file", "test.exe")
        
        $uploadResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/upload" -Method POST -Headers $headers -Body $multipartContent
        Write-Host "EXE文件上传成功 (不应该成功)!" -ForegroundColor Red
        Write-Host "响应: $($uploadResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
    } catch {
        Write-Host "EXE文件上传失败 (预期结果): $($_.Exception.Message)" -ForegroundColor Green
    }
}

Write-Host ""

# 7. 测试未登录访问
Write-Host "=== 测试7: 未登录访问文件列表 ===" -ForegroundColor Green
try {
    $listResponse = Invoke-RestMethod -Uri "http://localhost:7676/api/file/list" -Method GET
    Write-Host "未登录访问成功 (不应该成功)!" -ForegroundColor Red
    Write-Host "响应: $($listResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Cyan
} catch {
    Write-Host "未登录访问失败 (预期结果): $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host ""

Write-Host "=== 测试完成 ===" -ForegroundColor Green
