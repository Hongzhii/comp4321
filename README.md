# Web Crawler and Inverted Indexer

This project is a web crawler and inverted indexer that extracts information from web pages and creates an inverted index for efficient keyword-based searching.

## Prerequisites

To compile and run this project, you need to have the following dependencies installed. Clone the repo, and the files below will automatically be included

- Java Development Kit (JDK)
- htmlparser.jar
- jsoup-1.17.2.jar
- jdbm-1.0.jar

## Compilation

To compile the project, use the following command:

```shell
javac -cp htmlparser.jar:jsoup-1.17.2.jar:jdbm-1.0.jar:. *.java
```
## Running the Inverter

To run the program, use the following command

```shell
java -cp htmlparser.jar:jsoup-1.17.2.jar:jdbm-1.0.jar:. PROJECT.Inverter "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
```


Running other java files for testing (e.g: SearchEngine)

```shell
java -cp htmlparser.jar:jsoup-1.17.2.jar:jdbm-1.0.jar:. PROJECT.SearchEngine
```


## Viewing result
After running the Inverter program, the extracted information and inverted index will be stored in the spider_result.txt file. You can view the contents of the file using the following command:


```
cat spider_result.txt
```


## License
This project is licensed under the MIT License.
Feel free to copy and paste the above content into your README file, making any necessary adjustments or additions.
