package com.moneymoney.web.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.moneymoney.web.entity.Transaction;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
@EnableHystrix
@Controller
public class BankAppController {
	private static CurrentDataSet storeCurrentDataSet;
	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping("/")
	public String depositForm() {
		return "DepositForm";
	}

	@RequestMapping("/deposit")
	@HystrixCommand(fallbackMethod = "fallbackDeposit")
	public String deposit(@ModelAttribute Transaction transaction, Model model) {
		System.out.println(transaction.getAccountNumber() + " - " + transaction.getAmount());
		restTemplate.postForEntity("http://localhost:9090/transaction/transactions/deposit", transaction, null);
		model.addAttribute("message", "Success!");
		return "DepositForm";
	}
	public String fallbackDeposit(@ModelAttribute Transaction transaction, Model model) {
		model.addAttribute("message", "Transaction Service is out of service!!!");
		return "DepositForm";
	}

	@RequestMapping("/demo")
	public String demo(Model model) {
		ResponseEntity<String> string = restTemplate.getForEntity("http://localhost:9090/transaction/transactions/demo", String.class);
		System.out.println(string.getBody());
		model.addAttribute("message", string.getBody());
		return "DepositForm";
	}

	@RequestMapping("/WITHDRAW")
	public String withdrawForm() {
		return "WithdrawForm";
	}

	@RequestMapping("/withdraw")
	@HystrixCommand(fallbackMethod = "fallbackWithdraw")
	public String withdraw(@ModelAttribute Transaction transaction, Model model) {
		restTemplate.postForEntity("http://localhost:9090/transaction/transactions", transaction, null);
		model.addAttribute("message", "Success!");
		return "WithdrawForm";
	}
	public String fallbackWithdraw(@ModelAttribute Transaction transaction, Model model) {
		model.addAttribute("message", "Transaction Service is out of service!!!");
		return "WithdrawForm";
	}

	@RequestMapping("/FUNDTRANSFER")
	public String fundTransferForm() {
		return "fundTransferForm";
	}

	@RequestMapping("/fundTransfer")
	@HystrixCommand(fallbackMethod = "fallbackFundTransfer")
	public String fundTransfer(@RequestParam("senderAccountNumber") String senderAccountNumber,
			@RequestParam("amount") String amount, @RequestParam("receiverAccountNumber") String receiverAccountNumber,
			Model model) {
		restTemplate
				.postForEntity(
						"http://localhost:9090/transaction/transactions/fundtransfer?senderAccountNumber=" + senderAccountNumber
								+ "&&receiverAccountNumber=" + receiverAccountNumber + "&&amount=" + amount,
						null, null);
		model.addAttribute("message", "Success!");
		return "fundTransferForm";
	}
	
	public String fallbackFundTransfer(@RequestParam("senderAccountNumber") String senderAccountNumber,
			@RequestParam("amount") String amount, @RequestParam("receiverAccountNumber") String receiverAccountNumber,
			Model model) {
		model.addAttribute("message", "Transaction Service is out of service!!!");
		return "fundTransferForm";
	}

	/*
	 * @RequestMapping("/getAll") public String getAllTransactionsForm() { return
	 * "getAll"; }
	 */

	/*
	 * @RequestMapping("/getAll") public String getAllTransactions(Model model) {
	 * ResponseEntity<List> t =
	 * restTemplate.getForEntity("http://localhost:9090/transaction/transactions",
	 * List.class); List<Transaction> transactionList = t.getBody();
	 * System.out.println(transactionList); model.addAttribute("transactionList",
	 * transactionList); model.addAttribute("message", "Success!"); return "getAll";
	 * }
	 */

	/*
	 * @RequestMapping("/statement") public ModelAndView
	 * getStatement(@RequestParam("offset") int offset, @RequestParam("size") int
	 * size) { int currentSize = size == 0 ? 5 : size; int currentOffset = offset ==
	 * 0 ? 1 : offset; Link previous =
	 * ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(BankAppController
	 * .class) .getStatement(currentOffset - currentSize,
	 * currentSize)).withRel("previous"); Link next =
	 * ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(BankAppController
	 * .class) .getStatement(currentOffset + currentSize,
	 * currentSize)).withRel("next"); CurrentDataSet currentDataSet =
	 * restTemplate.getForObject("http://localhost:9090/transaction/transactions",
	 * CurrentDataSet.class); List<Transaction> transactionList =
	 * currentDataSet.getTransaction(); List<Transaction> transactions = new
	 * ArrayList<Transaction>(); for (int value = currentOffset - 1; value <
	 * currentOffset + currentSize - 1; value++) { if ((transactionList.size() <=
	 * value && value > 0) || currentOffset < 1) break; Transaction transaction =
	 * transactionList.get(value); transactions.add(transaction); }
	 * currentDataSet.setPrevLink(previous); currentDataSet.setNextLink(next);
	 * currentDataSet.setTransaction(transactions); return new
	 * ModelAndView("statements", "currentDataSet", currentDataSet); }
	 */
	@RequestMapping("/statement")
	@HystrixCommand(fallbackMethod = "fallbackStatement")
	public ModelAndView getStatement(@RequestParam("offset") int offset, @RequestParam("size") int size) {
		
		System.out.println("start of statements");
		CurrentDataSet currentDataSet = restTemplate.getForObject("http://localhost:9090/transaction/transactions/currentDataSet",
				CurrentDataSet.class);
		System.out.println(currentDataSet);
		int currentSize = size == 0 ? 5 : size;
		int currentOffset = offset == 0 ? 1 : offset;
		Link next = linkTo(
				methodOn(BankAppController.class).getStatement(currentOffset + currentSize, currentSize))
						.withRel("next");
		Link previous = linkTo(
				methodOn(BankAppController.class).getStatement(currentOffset - currentSize, currentSize))
						.withRel("previous");
		List<Transaction> transactions = currentDataSet.getTransactions();
		List<Transaction> currentDataSetList = new ArrayList<Transaction>();
		storeCurrentDataSet = currentDataSet;
		for (int i = currentOffset - 1; i < currentSize + currentOffset - 1; i++) {
			if ((transactions.size() <= i && i > 0) || currentOffset < 1)
				break;
			Transaction transaction = transactions.get(i);
			currentDataSetList.add(transaction);

		}
		CurrentDataSet dataSet = new CurrentDataSet(currentDataSetList, previous,next);
		ModelAndView modelView = new ModelAndView();
		modelView.addObject("currentDataSet", dataSet);
		modelView.setViewName("statements");
		return modelView;
	}
	public ModelAndView fallbackStatement(@RequestParam("offset") int offset, @RequestParam("size") int size) {
		//model.addAttribute("message", "Transaction Service is out of service!!!");
		ModelAndView modelView = new ModelAndView();
		modelView.addObject("message", "Transaction Service is out of service!!!");
		modelView.setViewName("statements");
		return modelView;
	}
	
}
