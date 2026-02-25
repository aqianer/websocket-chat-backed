# 文件上传接口测试脚本（使用curl）

$token = "97385bd6-3eee-481f-81b7-6cf31ddfe8f8"
$baseUrl = "http://localhost:7676/api/file"

Write-Host "=== 文件上传接口测试 ===" -ForegroundColor Green
Write-Host "Token: $token" -ForegroundColor Yellow
Write-Host ""

# 1. 测试单文件上传 - TXT文件
Write-Host "=== 测试1: 单文件上传 - TXT文件 ===" -ForegroundColor Green
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "file=@test-files/test.txt" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 2. 测试单文件上传 - PDF文件
Write-Host "=== 测试2: 单文件上传 - PDF文件 ===" -ForegroundColor Green
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "file=@test-files/test.pdf" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 3. 测试单文件上传 - DOCX文件
Write-Host "=== 测试3: 单文件上传 - DOCX文件 ===" -ForegroundColor Green
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "file=@test-files/test.docx" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 4. 测试批量文件上传
Write-Host "=== 测试4: 批量文件上传 ===" -ForegroundColor Green
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "files=@test-files/test.txt" -F "files=@test-files/test.pdf" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 5. 测试不支持的文件类型
Write-Host "=== 测试5: 不支持的文件类型 (exe) ===" -ForegroundColor Green
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "file=@test-files/test.exe" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 6. 测试未登录访问
Write-Host "=== 测试6: 未登录访问 ===" -ForegroundColor Green
$result = curl.exe -X GET "$baseUrl/list"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

Write-Host "=== 测试完成 ===" -ForegroundColor Green
