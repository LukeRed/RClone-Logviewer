import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.util.Callback
import java.util.*


class LogViewerApp: Application() {
    private val table: TableView<Table> = TableView()
    var folderImage: Image = Image(
        javaClass.getResourceAsStream("folder.png")
    )
    var addImage: Image = Image(
        javaClass.getResourceAsStream("add.png")
    )
    var removeImage: Image = Image(
        javaClass.getResourceAsStream("file.png")
    )
    var changeImage: Image = Image(
        javaClass.getResourceAsStream("paper.png")
    )

    val refreshButton = Button("Refresh")
    val textField = TextField()

    override fun start(stage: Stage?) {
        stage?: return
        val scene = Scene(Group())
        stage.title = "Cloud Backup Logviewer"

        val label = Label("Backups")
        label.font = Font("Arial", 20.0)

        table.isEditable = true

        val dateColumn = TableColumn<Table, Date>("Date")
        dateColumn.cellValueFactory = PropertyValueFactory<Table, Date>("date")
        val driveColumn = TableColumn<Table, String>("Drive")
        driveColumn.cellValueFactory = PropertyValueFactory<Table, String>("drive")

        val statisticsColumn = TableColumn<Table, Any>("Statistics")
        val totalColumn = TableColumn<Table, Long>("Total")
        totalColumn.cellValueFactory = PropertyValueFactory<Table, Long>("total")
        totalColumn.cellFactory = Callback<TableColumn<Table, Long>, TableCell<Table, Long?>> {
            object: TableCell<Table, Long?>() {
                override fun updateItem(bytes: Long?, empty: Boolean) {
                    text = if (empty) { "" } else { toHRBytes(bytes!!) }
                }
            }
        }
        val timeColumn = TableColumn<Table, Double>("Time")
        timeColumn.cellValueFactory = PropertyValueFactory<Table, Double>("time")
        timeColumn.cellFactory = Callback<TableColumn<Table, Double>, TableCell<Table, Double?>> {
            object: TableCell<Table, Double?>() {
                override fun updateItem(time: Double?, empty: Boolean) {
                    text = if (empty) { "" } else { toHRTime(time!!) }
                }
            }
        }
        val speedColumn = TableColumn<Table, Long>("Speed")
        speedColumn.cellValueFactory = PropertyValueFactory<Table, Long>("speed")
        speedColumn.cellFactory = Callback<TableColumn<Table, Long>, TableCell<Table, Long?>> {
            object: TableCell<Table, Long?>() {
                override fun updateItem(bytes: Long?, empty: Boolean) {
                    text = if (empty) { "" } else { "${toHRBytes(bytes!!)}/s" }
                }
            }
        }
        statisticsColumn.columns.addAll(totalColumn, timeColumn, speedColumn)

        val filesColumn = TableColumn<Table, Any>("Files")
        val newColumn = TableColumn<Table, Int>("New")
        newColumn.cellValueFactory = PropertyValueFactory<Table, Int>("new")
        newColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int>> {
            GreyIntCell()
        }
        val changedColumn = TableColumn<Table, Int>("Changed")
        changedColumn.cellValueFactory = PropertyValueFactory<Table, Int>("changed")
        changedColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int>> {
            GreyIntCell()
        }
        val deletedColumn = TableColumn<Table, Int>("Deleted")
        deletedColumn.cellValueFactory = PropertyValueFactory<Table, Int>("deleted")
        deletedColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int>> {
            GreyIntCell()
        }
        filesColumn.columns.addAll(newColumn, changedColumn, deletedColumn)

        val statusColumn = TableColumn<Table, Any>("Status")
        val errorsColumn = TableColumn<Table, Int>("Errors")
        errorsColumn.cellValueFactory = PropertyValueFactory<Table, Int>("errors")
        errorsColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int?>> {
            object: TableCell<Table, Int?>() {
                override fun updateItem(value: Int?, empty: Boolean) {
                    value?:return
                    text = value.toString()
                    style = "-fx-text-fill: ${if(value > 0){"red"}else{"black"}};"
                }
            }
        }
        val transferredColumn = TableColumn<Table, Int>("Transferred")
        transferredColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int>> {
            GreyIntCell()
        }
        transferredColumn.cellValueFactory = PropertyValueFactory<Table, Int>("transferred")
        val checksColumn = TableColumn<Table, Int>("Checks")
        checksColumn.cellValueFactory = PropertyValueFactory<Table, Int>("checks")
        checksColumn.cellFactory = Callback<TableColumn<Table, Int>, TableCell<Table, Int>> {
            GreyIntCell()
        }
        statusColumn.columns.addAll(transferredColumn, errorsColumn, checksColumn)

        table.columns.addAll(dateColumn, driveColumn, statisticsColumn, statusColumn, filesColumn)
        table.rowFactory = Callback<TableView<Table>, TableRow<Table>> {
            val row = TableRow<Table>()
            row.onMouseClicked = EventHandler {
                if (it.clickCount == 2 && !row.isEmpty) {
                    showDetails(stage, row.item)
                }
            }
            row
        }


        refreshButton.onMouseReleased = EventHandler<MouseEvent> {
            refreshButton.isDisable = true
            refreshData()
        }
        val label1 = Label("Log Folder:")
        textField.text = "./"
        val hbox = HBox()
        hbox.spacing = 10.0
        hbox.children.addAll(label1, textField, refreshButton)
        val pane = AnchorPane()
        setAnchors(table, 35.0,10.0,10.0,10.0)
        setAnchors(label, 10.0,null,null, 10.0)
        setAnchors(hbox, 5.0,10.0,null, null)
        pane.children.addAll(label, hbox, table)
        refreshData()
        scene.root = pane
        stage.scene = scene
        stage.show()
        stage.onHidden = EventHandler {
            Platform.exit()
        }

    }

    private fun showDetails(stage: Stage, data: Table) {
        val label = Label("Backups Details")
        label.font = Font("Arial", 20.0)

        val secondaryLayout = AnchorPane()
        val splitPane = SplitPane()
        val detailPane = StackPane()
        val treeData = createTree(data)
        val tree = TreeView<String>(toTreeItem(treeData))
        setAnchors(splitPane, 30.0,10.0,10.0,10.0)
        setAnchors(label, 5.0,null,null, 10.0)
        secondaryLayout.children.addAll(label, splitPane)
        splitPane.items.addAll(tree)

        val secondScene = Scene(secondaryLayout)

        // New window (Stage)
        // New window (Stage)
        val newWindow = Stage()
        newWindow.title = "Backup of ${data.drive} on ${data.date}"
        newWindow.scene = secondScene

        // Set position of second window, related to primary window.
        // Set position of second window, related to primary window.
        newWindow.x = stage.x + 200
        newWindow.y = stage.y + 100
        newWindow.show()
    }

    private fun refreshData() {
        val path = textField.text
        object : Thread() {
            // runnable for that thread
            override fun run() {
                try {
                    scanAndCheck(path)
                } finally {
                    Platform.runLater(Runnable { loadData() })
                }
            }
        }.start()
    }
    private fun loadData() {
        refreshButton.isDisable = true
        val data = FXCollections.observableArrayList<Table>()
        for (logFile in logFiles) {
            data.add(Table(logFile.date, logFile.drive.replace("backup",""), logFile.total, logFile.duration,
                logFile.averageSpeed, logFile.transferred, logFile.errors, logFile.checks, logFile.added.size,
                logFile.changed.size, logFile.deleted.size, logFile))
        }
        table.items = data
        refreshButton.isDisable = false
    }

    private fun toTreeItem(node: TreeNode): TreeItem<String> {
        val item = if(node.folder) {
            TreeItem("${node.name} (+${node.filesAdded} | ~${node.filesUpdated} | -${node.filesRemoved})", ImageView(folderImage))
        } else {
            TreeItem(node.name,
                when(node.type) {
                    1 -> ImageView(addImage)
                    2 -> ImageView(removeImage)
                    3 -> ImageView(changeImage)
                    else -> null
                }
            )
        }
        for ((s, treeNode) in node.children) {
            item.children.add(toTreeItem(treeNode))
        }
        return item
    }
}


fun createTree(data: Table): TreeNode {
    val rootElement = TreeNode(data.drive, true)
    for (file in data.logFile.added) {
        treeRecursive(rootElement, file.split("/"), 1)
    }
    for (file in data.logFile.deleted) {
        treeRecursive(rootElement, file.split("/"), 2)
    }
    for (file in data.logFile.changed) {
        treeRecursive(rootElement, file.split("/"), 3)
    }
    return rootElement
}
fun treeRecursive(node: TreeNode, path: List<String>, type:Int ) {
    val currentFolder = path[0]
    val nextPath = path.subList(1, path.size)
    var next = node.children[currentFolder]
    if (next == null) {
        next = TreeNode(currentFolder, nextPath.isNotEmpty(), type=type)
        node.children[currentFolder] = next
    }
    if (nextPath.isNotEmpty()) {
        treeRecursive(next, nextPath, type)
    } else {
        when(type) {
            1 -> next.filesAdded++
            2 -> next.filesRemoved++
            3 -> next.filesUpdated++
        }
    }
    node.filesAdded = 0
    node.filesRemoved = 0
    node.filesUpdated = 0
    for ((s, treeNode) in node.children) {
        node.filesAdded += treeNode.filesAdded
        node.filesRemoved += treeNode.filesRemoved
        node.filesUpdated += treeNode.filesUpdated
    }

}


data class TreeNode(val name: String, val folder: Boolean, var filesAdded: Int = 0, var filesRemoved: Int = 0,
                    var filesUpdated: Int = 0,  val children: MutableMap<String, TreeNode> = mutableMapOf(), val type: Int = 0
)

class GreyIntCell<T>: TableCell<T, Int>() {
    override fun updateItem(value: Int?, empty: Boolean) {
        super.updateItem(value, empty)
        value?: return;
        text = value.toString()
        style = if (!empty && value == 0) {
            "-fx-text-fill: grey;"
        } else {
            "-fx-text-fill: black;"
        }
    }
}

fun setAnchors(element: Node, top: Double?, right: Double?, bottom: Double?, left: Double?) {
    top?.let { AnchorPane.setTopAnchor(element, it) }
    left?.let { AnchorPane.setLeftAnchor(element, it) }
    right?.let { AnchorPane.setRightAnchor(element, it) }
    bottom?.let { AnchorPane.setBottomAnchor(element, it) }
}

data class Table(
    val date: Date, val drive: String, val total: Long, val time: Double, val speed: Long,
    val transferred: Int, val errors: Int, val checks: Int, val new: Int, val changed: Int, val deleted: Int, val logFile: LogFile)