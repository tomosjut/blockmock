#!/bin/bash
# Test script voor SFTP endpoint demo

echo "======================================"
echo "BlockMock SFTP Demo Tests"
echo "======================================"
echo ""

SFTP_HOST="localhost"
SFTP_PORT="2222"
SFTP_USER="testuser"
SFTP_PASS="testpass"

# Create test file
echo "This is a test file uploaded via SFTP" > /tmp/demo-upload.txt
echo "Line 2: Transaction data: TXN-12345" >> /tmp/demo-upload.txt
echo "Line 3: Amount: 199.99 EUR" >> /tmp/demo-upload.txt

echo "1. Upload file via SFTP"
echo "--------------------------------------"
echo "Uploading /tmp/demo-upload.txt to /uploads/"
sshpass -p "${SFTP_PASS}" sftp -o StrictHostKeyChecking=no -P ${SFTP_PORT} ${SFTP_USER}@${SFTP_HOST} <<EOF
cd /uploads
put /tmp/demo-upload.txt
ls -la
bye
EOF
echo ""
echo ""

echo "2. Download file via SFTP"
echo "--------------------------------------"
echo "Downloading /downloads/report.pdf"
sshpass -p "${SFTP_PASS}" sftp -o StrictHostKeyChecking=no -P ${SFTP_PORT} ${SFTP_USER}@${SFTP_HOST} <<EOF
cd /downloads
get report.pdf /tmp/demo-download.pdf
ls -la
bye
EOF

if [ -f "/tmp/demo-download.pdf" ]; then
    echo "✓ File downloaded successfully"
    ls -lh /tmp/demo-download.pdf
else
    echo "✗ File download failed"
fi
echo ""
echo ""

echo "3. List directory via SFTP"
echo "--------------------------------------"
sshpass -p "${SFTP_PASS}" sftp -o StrictHostKeyChecking=no -P ${SFTP_PORT} ${SFTP_USER}@${SFTP_HOST} <<EOF
ls -la /
ls -la /uploads
bye
EOF
echo ""
echo ""

echo "======================================"
echo "SFTP Tests Completed!"
echo "======================================"
echo ""
echo "Note: Install sshpass if not available:"
echo "  Ubuntu/Debian: sudo apt-get install sshpass"
echo "  macOS: brew install sshpass"
echo ""
echo "Check Request Logs in UI: http://localhost:8888"
echo "======================================"
