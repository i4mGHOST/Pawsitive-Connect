const express = require('express');
const router = express.Router();
const {
    getTransactions,
    createTransaction,
    payPendingTransaction,
    cancelPendingTransaction,
    processPayment
} = require('../controllers/transactionController');

router.get('/', getTransactions);
router.post('/', createTransaction);
router.put('/:id/pay', payPendingTransaction);
router.put('/:id/cancel', cancelPendingTransaction);
router.post('/process', processPayment);

module.exports = router;