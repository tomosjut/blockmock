FROM alpine:latest

# Install OpenSSH server
RUN apk add --no-cache openssh-server openssh-sftp-server curl

# Create SFTP user and set up directories
RUN adduser -D -s /bin/sh sftpuser && \
    echo "sftpuser:testpass" | chpasswd && \
    mkdir -p /home/sftpuser/uploads /home/sftpuser/downloads && \
    chown -R sftpuser:sftpuser /home/sftpuser

# Generate host keys
RUN ssh-keygen -A

# Configure SSHD for SFTP
RUN echo "Port 2222" >> /etc/ssh/sshd_config && \
    echo "PermitRootLogin no" >> /etc/ssh/sshd_config && \
    echo "PasswordAuthentication yes" >> /etc/ssh/sshd_config && \
    echo "ChallengeResponseAuthentication no" >> /etc/ssh/sshd_config && \
    echo "UsePAM yes" >> /etc/ssh/sshd_config && \
    echo "X11Forwarding no" >> /etc/ssh/sshd_config && \
    echo "PrintMotd no" >> /etc/ssh/sshd_config && \
    echo "AcceptEnv LANG LC_*" >> /etc/ssh/sshd_config && \
    echo "Subsystem sftp /usr/lib/ssh/sftp-server" >> /etc/ssh/sshd_config && \
    echo "" >> /etc/ssh/sshd_config && \
    echo "# SFTP-only configuration for sftpuser" >> /etc/ssh/sshd_config && \
    echo "Match User sftpuser" >> /etc/ssh/sshd_config && \
    echo "    ForceCommand internal-sftp" >> /etc/ssh/sshd_config && \
    echo "    ChrootDirectory /home/sftpuser" >> /etc/ssh/sshd_config && \
    echo "    PermitTunnel no" >> /etc/ssh/sshd_config && \
    echo "    AllowAgentForwarding no" >> /etc/ssh/sshd_config && \
    echo "    AllowTcpForwarding no" >> /etc/ssh/sshd_config && \
    echo "    X11Forwarding no" >> /etc/ssh/sshd_config

# Fix permissions for chroot
RUN chmod 755 /home/sftpuser && \
    chown root:root /home/sftpuser

# Create mock PDF file for downloads
RUN echo "%PDF-1.4" > /home/sftpuser/downloads/report.pdf && \
    echo "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj" >> /home/sftpuser/downloads/report.pdf && \
    echo "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj" >> /home/sftpuser/downloads/report.pdf && \
    echo "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj" >> /home/sftpuser/downloads/report.pdf && \
    echo "xref" >> /home/sftpuser/downloads/report.pdf && \
    echo "0 4" >> /home/sftpuser/downloads/report.pdf && \
    echo "trailer<</Size 4/Root 1 0 R>>startxref" >> /home/sftpuser/downloads/report.pdf && \
    echo "%%EOF" >> /home/sftpuser/downloads/report.pdf && \
    chmod 644 /home/sftpuser/downloads/report.pdf

# Expose SFTP port
EXPOSE 2222

# Start SSHD
CMD ["/usr/sbin/sshd", "-D", "-e"]
