#!/usr/bin/env python3
"""
Test script voor Apache Artemis endpoint demo
Gebruikt STOMP protocol (makkelijker dan JMS voor demo)

Installatie: pip install stomp.py
"""

import stomp
import json
import time
import sys

class MessageListener(stomp.ConnectionListener):
    def __init__(self):
        self.messages = []

    def on_message(self, frame):
        print(f"✓ Received message:")
        print(f"  Headers: {frame.headers}")
        print(f"  Body: {frame.body}")
        self.messages.append(frame.body)

    def on_error(self, frame):
        print(f"✗ Error: {frame.body}")

def test_artemis():
    print("=" * 50)
    print("BlockMock Apache Artemis Demo Tests")
    print("=" * 50)
    print()

    # Connect to Artemis
    print("1. Connecting to Artemis (STOMP)...")
    print("-" * 50)

    conn = stomp.Connection([('localhost', 61613)])
    listener = MessageListener()
    conn.set_listener('', listener)

    try:
        conn.connect('admin', 'admin', wait=True)
        print("✓ Connected to Artemis")
        print()

        # Subscribe to queue
        print("2. Subscribing to demo.notification.queue...")
        print("-" * 50)
        conn.subscribe(destination='/queue/demo.notification.queue', id=1, ack='auto')
        print("✓ Subscribed")
        print()

        # Send test message
        print("3. Sending test notification...")
        print("-" * 50)

        notification = {
            "type": "sms",
            "to": "+31612345678",
            "message": "Your order has been shipped!",
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        conn.send(
            destination='/queue/demo.notification.queue',
            body=json.dumps(notification),
            headers={'content-type': 'application/json'}
        )

        print(f"✓ Sent notification: {json.dumps(notification, indent=2)}")
        print()

        # Wait for response from BlockMock
        print("4. Waiting for messages (5 seconds)...")
        print("-" * 50)
        time.sleep(5)

        if listener.messages:
            print(f"✓ Received {len(listener.messages)} message(s)")
        else:
            print("⚠ No messages received (check if BlockMock endpoint is enabled)")
        print()

        # Disconnect
        conn.disconnect()
        print("✓ Disconnected from Artemis")

    except Exception as e:
        print(f"✗ Error: {e}")
        print()
        print("Troubleshooting:")
        print("  1. Is Artemis running? docker ps | grep artemis")
        print("  2. Is BlockMock endpoint enabled?")
        print("  3. Check Artemis console: http://localhost:8161/console")
        sys.exit(1)

    print()
    print("=" * 50)
    print("Artemis Tests Completed!")
    print("Check Request Logs in UI: http://localhost:8888")
    print("=" * 50)

if __name__ == '__main__':
    try:
        import stomp
    except ImportError:
        print("Error: stomp.py not installed")
        print("Install with: pip install stomp.py")
        sys.exit(1)

    test_artemis()
