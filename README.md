# Wallet Service

## Запуск


1. Склонируйте репозиторий и перейдите в его директорию:

```
git clone https://github.com/rbrmnv/wallet-service.git
cd wallet-service
```

2. Скопируйте файл с переменными окружения:

```
cp .env.example .env
```

3. Запустите сборку и поднятие сервиса:

```
docker compose up --build
```

## Эндпоинты

### Перевод средств

`POST /api/transactions`

Создаётся перевод между счетами

Тело запроса:

```json
{
  "idempotencyKey": "31111111-1111-1111-1111-111111111111",
  "fromWalletId": "1ba9646b-962c-4d67-bf0a-c73c2615293a",
  "toWalletId": "fe84a10c-a6b5-4d4f-bae6-9818f2b8ed69",
  "amount": 100.00
}
```

Ответ `201 Created`:

```json
{
  "id": "49690110-f8b5-44bf-b427-81d065bc4f45",
  "fromWalletId": "1ba9646b-962c-4d67-bf0a-c73c2615293a",
  "toWalletId": "fe84a10c-a6b5-4d4f-bae6-9818f2b8ed69",
  "amount": 100.00,
  "type": "PAYMENT",
  "createdAt": "2026-06-28T09:13:09.458915Z"
}
```

### Список счетов

`GET /api/wallets`

Возвращаются все счета с их текущими балансами и валютой

Ответ `200 OK`:

```json
[
  {
    "id": "bac0b08c-ef8f-438b-9edf-2701cb437f04",
    "balance": 5768.00,
    "currency": "RUB"
  },
  {
    "id": "7395e17a-c5c8-4afd-91d8-8e59fb4b0a65",
    "balance": 1624.40,
    "currency": "RUB"
  }
  
  ...
  
]
```

### История по счёту

`GET /api/reports/balance/{walletId}`

Возвращается счёт и список его транзакций

Ответ `200 OK`:

```json
{
  "walletResponse": {
    "id": "1ba9646b-962c-4d67-bf0a-c73c2615293a",
    "balance": 5782.27,
    "currency": "USD"
  },
  "listOfTTransactionResponses": [
    {
      "id": "49690110-f8b5-44bf-b427-81d065bc4f45",
      "fromWalletId": "1ba9646b-962c-4d67-bf0a-c73c2615293a",
      "toWalletId": "fe84a10c-a6b5-4d4f-bae6-9818f2b8ed69",
      "amount": 100.00,
      "type": "PAYMENT",
      "createdAt": "2026-06-28T09:13:09.458915Z"
    }
  ]
}
```

### Сводный отчёт

`GET /api/reports/summary`

Возвращаются общая сумма средств в системе, сгруппированная
по валютам, и количество успешных транзакций

Ответ `200 OK`:

```json
{
  "totalByCurrency": {
    "RUB": 49951.07,
    "USD": 15623.87,
    "EUR": 5121.62
  },
  "transactionsToday": 7
}
```