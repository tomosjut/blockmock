/**
 * BlockMock AMQP demo — order service
 *
 * Subscribes to order.incoming, processes the order, publishes order.confirmed.
 * Connect both this app and BlockMock to the same Artemis broker (docker-compose).
 */

const { Connection } = require('rhea-promise')

const AMQP_HOST     = process.env.AMQP_HOST     || 'localhost'
const AMQP_PORT     = parseInt(process.env.AMQP_PORT || '5672')
const AMQP_USER     = process.env.AMQP_USER     || 'artemis'
const AMQP_PASSWORD = process.env.AMQP_PASSWORD || 'artemis'

const INCOMING_ADDRESS  = process.env.INCOMING_ADDRESS  || 'order.incoming'
const CONFIRMED_ADDRESS = process.env.CONFIRMED_ADDRESS || 'order.confirmed'

async function main() {
  const connection = new Connection({
    hostname: AMQP_HOST,
    port: AMQP_PORT,
    username: AMQP_USER,
    password: AMQP_PASSWORD,
    reconnect: true,
  })

  await connection.open()
  console.log(`Connected to AMQP broker at ${AMQP_HOST}:${AMQP_PORT}`)

  const sender = await connection.createSender({
    target: { address: CONFIRMED_ADDRESS },
  })
  console.log(`Sender ready on: ${CONFIRMED_ADDRESS}`)

  const receiver = await connection.createReceiver({
    source: { address: INCOMING_ADDRESS },
  })
  console.log(`Listening for orders on: ${INCOMING_ADDRESS}`)
  console.log('Waiting for messages...\n')

  receiver.on('message', async (context) => {
    const rawBody = context.message.body
    console.log(`[order.incoming] Received:`, rawBody)

    let order
    try {
      order = typeof rawBody === 'string' ? JSON.parse(rawBody) : rawBody
    } catch {
      console.error('Could not parse message body as JSON, skipping')
      return
    }

    const orderId = `ORD-${Date.now()}`
    console.log(`Processing order ${orderId} for customer: ${order.customerId}`)

    // Simulate order processing (e.g. call payment/inventory services)
    await new Promise(r => setTimeout(r, 50))

    const confirmedEvent = {
      orderId,
      customerId: order.customerId,
      status: 'confirmed',
      items: order.items ?? [],
      totalAmount: order.totalAmount,
      confirmedAt: new Date().toISOString(),
    }

    await sender.send({ body: JSON.stringify(confirmedEvent) })
    console.log(`[order.confirmed] Published:`, confirmedEvent)
    console.log()
  })

  process.on('SIGINT', async () => {
    console.log('\nShutting down...')
    await receiver.close()
    await sender.close()
    await connection.close()
    process.exit(0)
  })
}

main().catch(err => {
  console.error('Fatal error:', err.message)
  process.exit(1)
})
