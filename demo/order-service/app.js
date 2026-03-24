const express = require('express')
const app = express()
app.use(express.json())

const BLOCKMOCK_URL = process.env.BLOCKMOCK_URL || 'http://localhost:8080'
const PORT = process.env.PORT || 3000

async function callService(name, url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const data = await res.json().catch(() => ({}))
  console.log(`[${name}] ${res.status}`, data)
  return { status: res.status, body: data }
}

app.post('/orders', async (req, res) => {
  const { customerId, items, totalAmount } = req.body

  if (!customerId || !items || !totalAmount) {
    return res.status(400).json({ error: 'customerId, items, and totalAmount are required' })
  }

  const orderId = `ORD-${Date.now()}`
  console.log(`Processing order ${orderId} for customer ${customerId}`)

  try {
    // 1. Charge payment
    const payment = await callService('payment', `${BLOCKMOCK_URL}/mock/api/payment/charge`, {
      customerId,
      amount: totalAmount,
      currency: 'EUR',
    })
    if (payment.status !== 200) {
      return res.status(402).json({ error: 'Payment failed', detail: payment.body })
    }

    // 2. Reserve inventory
    const inventory = await callService('inventory', `${BLOCKMOCK_URL}/mock/api/inventory/reserve`, {
      orderId,
      items,
    })
    if (inventory.status !== 200) {
      return res.status(409).json({ error: 'Inventory reservation failed', detail: inventory.body })
    }

    // 3. Send notification
    await callService('notifications', `${BLOCKMOCK_URL}/mock/api/notifications/send`, {
      customerId,
      message: `Your order ${orderId} has been placed successfully.`,
      channel: 'email',
    })

    res.status(201).json({
      orderId,
      customerId,
      items,
      totalAmount,
      status: 'confirmed',
      paymentRef: payment.body.transactionId ?? null,
      inventoryRef: inventory.body.reservationId ?? null,
    })
  } catch (err) {
    console.error('Order processing error:', err.message)
    res.status(500).json({ error: 'Internal error', detail: err.message })
  }
})

app.get('/health', (_req, res) => res.json({ status: 'ok' }))

app.listen(PORT, () => {
  console.log(`Order service listening on http://localhost:${PORT}`)
  console.log(`Calling BlockMock at ${BLOCKMOCK_URL}`)
})
