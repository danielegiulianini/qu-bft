package qu.view

import qu.controller.ViewObserver

class ViewImpl extends View {


  override def display(): Unit = ???

  override def setObserver(observer: ViewObserver): Unit = ???

  override def start(): Unit = {
    /*
      def run(): Unit = { // Create object of Scanner class so that we can take input from command-line
    val myScanner = new Nothing(System.in)
    var inputLine = null
    // Show prompt
    this.generalPrompt
    while ( {
      myScanner.hasNextLine
    }) { // Scan next line from command-prompt
      inputLine = myScanner.nextLine
      // Deceide which operation to do...
      if (inputLine == "insert") {
        System.out.print("name contact email address: ")
        val query = myScanner.nextLine
        this.controller.insertOperation(query)
      }
      else if (inputLine == "udpate") {
        // code for update. Sample code may look like:
        //                this.controller.updateOperation();
      }
      else if (inputLine == "quit") { // Bye bye...
        break //todo: break is not supported

      }
      // Display Prompt
      this.generalPrompt
    }
  }
     */
  }
}
