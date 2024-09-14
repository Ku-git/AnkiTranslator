# AnkiTransfer

<h3> 需要條件 </h3>

- Anki app (AnkiWeb 無法執行操作)
- 需要下載AnkiConnect (可以在Anki Add-on找到)
- 目前使用Google Cloud Transfer API，需要Google cloud的API KEY，需要自行去google cloud console中申請

<h3> 基本流程 </h3>

 1. 開啟Anki，目前使用Anki Windows因為AnkiWeb無法直接編輯，也無法做額外操作。 
    而Anki Windows則可以使用額外套件，也就是AnkiConnect套件，才可以執行使用其中的
    AnkiConnect的API才處理資料的CRUD(主要使用update)
 2. 使用Google Translate API，需要直接去google cloud申請API KEY。
 3. 針對你要更新的anki資料，先做備份，執行後就是更新後的內容。