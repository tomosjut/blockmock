#!/usr/bin/env python3
"""
Test script voor IBM MQ endpoint demo

Installatie: pip install pymqi
"""

import pymqi
import json
import time
import sys

def test_ibmmq():
    print("=" * 50)
    print("BlockMock IBM MQ Demo Tests")
    print("=" * 50)
    print()

    # Connection parameters
    queue_manager = 'QM1'
    channel = 'DEV.APP.SVRCONN'
    host = 'localhost'
    port = '1414'
    queue_name = 'DEMO.PAYMENT.QUEUE'
    conn_info = f'{host}({port})'
    user = 'app'
    password = 'passw0rd'

    qmgr = None

    try:
        print("1. Connecting to IBM MQ...")
        print("-" * 50)

        # Create connection
        cd = pymqi.CD()
        cd.ChannelName = channel.encode()
        cd.ConnectionName = conn_info.encode()
        cd.ChannelType = pymqi.CMQC.MQCHT_CLNTCONN
        cd.TransportType = pymqi.CMQC.MQXPT_TCP

        # Set credentials
        sco = pymqi.SCO()
        sco.KeyRepository = None

        qmgr = pymqi.connect(queue_manager, channel, conn_info, user, password)
        print(f"✓ Connected to Queue Manager: {queue_manager}")
        print()

        # Open queue for input/output
        print("2. Opening queue for read/write...")
        print("-" * 50)

        queue = pymqi.Queue(qmgr, queue_name)
        print(f"✓ Opened queue: {queue_name}")
        print()

        # Send payment request
        print("3. Sending payment request...")
        print("-" * 50)

        payment_request = {
            "payment_type": "credit_card",
            "card_number": "**** **** **** 1234",
            "amount": 149.99,
            "currency": "EUR",
            "merchant_id": "MERCH-12345",
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        message_body = json.dumps(payment_request)

        # Put message
        md = pymqi.MD()
        md.Format = pymqi.CMQC.MQFMT_STRING
        md.Persistence = pymqi.CMQC.MQPER_PERSISTENT
        md.MsgType = pymqi.CMQC.MQMT_REQUEST
        md.ReplyToQ = b'DEMO.REPLY.QUEUE'

        queue.put(message_body.encode('utf-8'), md)
        print(f"✓ Sent payment request:")
        print(json.dumps(payment_request, indent=2))
        print()

        # Wait for reply from BlockMock
        print("4. Waiting for payment response (auto-reply)...")
        print("-" * 50)

        gmo = pymqi.GMO()
        gmo.Options = pymqi.CMQC.MQGMO_WAIT | pymqi.CMQC.MQGMO_FAIL_IF_QUIESCING
        gmo.WaitInterval = 5000  # 5 seconds

        try:
            response_md = pymqi.MD()
            response_message = queue.get(None, response_md, gmo)

            print("✓ Received payment response:")
            print(f"  MsgId: {response_md.MsgId.hex()}")
            print(f"  CorrelId: {response_md.CorrelId.hex()}")
            print(f"  Body: {response_message.decode('utf-8')}")

            # Parse response
            try:
                response_data = json.loads(response_message.decode('utf-8'))
                print()
                print("  Parsed response:")
                print(json.dumps(response_data, indent=4))
            except:
                pass

        except pymqi.MQMIError as e:
            if e.comp == pymqi.CMQC.MQCC_FAILED and e.reason == pymqi.CMQC.MQRC_NO_MSG_AVAILABLE:
                print("⚠ No response received (check if BlockMock endpoint is enabled and auto-reply is on)")
            else:
                raise
        print()

        # Close queue
        queue.close()
        print("✓ Closed queue")

    except pymqi.MQMIError as e:
        print(f"✗ IBM MQ Error:")
        print(f"  Comp: {e.comp}")
        print(f"  Reason: {e.reason}")
        print()
        print("Troubleshooting:")
        print("  1. Is IBM MQ running? docker ps | grep ibmmq")
        print("  2. Is BlockMock endpoint enabled?")
        print("  3. Check MQ console: https://localhost:9443/ibmmq/console")
        print(f"  4. Check queue exists: {queue_name}")
        sys.exit(1)

    except Exception as e:
        print(f"✗ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

    finally:
        if qmgr:
            qmgr.disconnect()
            print("✓ Disconnected from IBM MQ")

    print()
    print("=" * 50)
    print("IBM MQ Tests Completed!")
    print("Check Request Logs in UI: http://localhost:8888")
    print("=" * 50)

if __name__ == '__main__':
    try:
        import pymqi
    except ImportError:
        print("Error: pymqi not installed")
        print()
        print("Installation instructions:")
        print("  Ubuntu/Debian:")
        print("    1. Download IBM MQ client: https://ibm.biz/mq-client-download")
        print("    2. Install: sudo dpkg -i ibm-mq-*-client.deb")
        print("    3. pip install pymqi")
        print()
        print("  Alternative: Use docker with pymqi pre-installed")
        sys.exit(1)

    test_ibmmq()
