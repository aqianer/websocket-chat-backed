$token = "97385bd6-3eee-481f-81b7-6cf31ddfe8f8"
$baseUrl = "http://localhost:7676/api/file"

Write-Host "=== 测试文件大小限制 ===" -ForegroundColor Green

# 测试超过10MB的文件
Write-Host "测试1: 上传超过10MB的文件 (11MB)" -ForegroundColor Yellow
$result = curl.exe -X POST -H "Authorization: Bearer $token" -F "file=@test-files/large-file.txt" "$baseUrl/upload"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""

# 测试文件列表
Write-Host "=== 测试2: 获取文件列表 ===" -ForegroundColor Green
$result = curl.exe -X GET -H "Authorization: Bearer $token" "$baseUrl/list"
Write-Host "响应: $result" -ForegroundColor Cyan
Write-Host ""
