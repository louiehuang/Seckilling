## Second Kill



This project is designed for the Second Kill promotion which is a scenario where millions of users snap up only a very limited number of products. For example, Xiaomi's seconds kill event may have only 10,000 mobile phones for special price, but there might be tens of millions of users trying to buy those mobile phones. As a result, the products were sold out within a second. 



### 1. Techs used in this project

#### Distribution

- Nginx: load balancing and separate static resources from dynamic requests

- Distributed session



#### For read requests

- Local Cache (using guava cache): First try to get item info from local cache on each application server

- Redis: If not found in local cache, go to redis
- DB: If still not found, get from DB



Things done outside this source code:

- Used CDN to handle static requests
- Cached dynamic requests
- Staticized webpages using PhantomJS



#### To reduce load of creating order

- Cached stock info
- Asynchronized order creating process
- Used asynchronous transaction



#### To smooth TPS

- Promo token for promotion activity
- Promo token threshold
- ExecutorService to limit the number of order creating requests handled at the same time



#### To prevent traffic burst

- Implemented captcha

- Used guava RateLimiter



### 2. Core logic

The logic of creating an order:

```java
OrderController:generateCaptcha() -> 
OrderController:generateToken() -> 
OrderController:createOrder() -> 
MQProducer:transactionAsyncDeductStock() -> 
OrderService:createOder() ->
MQConsumer
```



1 `OrderController:generateCaptcha()` When user clicks "buy" on the second kill page, generate captcha



2 `OrderController:generateToken()` User fills in the captcha to proceed, backend verifies captcha and generates promotion token



3 `OrderController:createOrder()` Front-end gets the promotion token and then finally submits order creating request. An Transactional  order creating message will be sent so as to make sure stock could be rolled back if there is any failure when processing the order. 



4 `MQProducer:transactionAsyncDeductStock()` Transcational message is done with `TransactionMQProducer` provided by RocketMQ. This is a 2-phase commit producer. If the message is sent successfully, the `TransactionListener#executeLocalTransaction()` method is called back to execute the local transaction, and the local transaction state is returned to LocalTransactionState with the following enumeration values:

- COMMIT_MESSAGE,
- ROLLBACK_MESSAGE,
- UNKNOW

Note: `TransactionListener#executeLocalTransaction()` executes the local transaction method and returns the local transaction status after the sender successfully sends the **PREPARED** message; if the PREPARED message fails to send, the `TransactionListener#executeLocalTransaction()` will not be invoked, and the local transaction message will be set to `LocalTransactionState.ROLLBACK_MESSAGE`, representing the message needs to be rolled back. `TransactionListener#checkLocalTransaction()` will be executed at intervals if the status remains `LocalTransactionState.UNKNOW`. 



Before the order is created successfully and written to DB, there is no record in DB, but we do need to track the order status for possible failure and then rollback. To do so, we created a table called `StockLog` in DB. 3 status are defined: 

- status=1 means initial status. 
- status=2 means the stock (in Redis) is deducted successfully. This also means that the order is created successfully in our project. 
- status=3 means something went wrong and stock info needs rolling back. 

Check the code in `OrderController:createOrder()`,  `MQProducer` and `OrderService:createOder()` to see how it works. 



5  `OrderService:createOder()` deducts stock in Redis, writes order record to DB and updates `StockLog` status. 



6 `MQConsumer` comsumes the message sent at step4 and deducts stock in DB. 

