Dim fso, folder, file, subfolder, xlApp, xlBook, ws
Dim saveFolderPath, workbookName, fileName
Dim rowNum, colNum, lastRow, lastCol, logSTR, cellValue, textStream
Dim csvRootFolder, relativePath

' Create FileSystemObject to handle folder and files
Set fso = CreateObject("Scripting.FileSystemObject")

' Get the folder where the script is located
folder = fso.GetParentFolderName(WScript.ScriptFullName)

' Define the root folder for CSV output
parentFolder = fso.GetParentFolderName(folder)
csvRootFolder = fso.BuildPath(parentFolder, "csvData")

' Create csvData folder if it doesn't exist
If Not fso.FolderExists(csvRootFolder) Then
    fso.CreateFolder(csvRootFolder)
End If

' Create Excel application object
Set xlApp = CreateObject("Excel.Application")
xlApp.Visible = False

' Function to process files in a folder and its subfolders
Sub ProcessFolder(currentFolder)
    For Each file In fso.GetFolder(currentFolder).Files
        ' Skip temporary files that start with "~$"
        If Left(file.Name, 2) = "~$" Then
            WScript.Echo "Skipping temporary file: " & file.Name
        ElseIf LCase(fso.GetExtensionName(file.Name)) = "xlsx" Then
            ProcessExcelFile file
        End If
    Next
    
    ' Process subfolders recursively
    For Each subfolder In fso.GetFolder(currentFolder).SubFolders
        ProcessFolder subfolder
    Next
End Sub

' Function to safely get cell value as string
Function GetCellValueAsString(cell)
    On Error Resume Next
    If IsEmpty(cell.Value) Then
        GetCellValueAsString = ""
    ElseIf IsNull(cell.Value) Then
        GetCellValueAsString = ""
    ElseIf IsObject(cell.Value) Then
        GetCellValueAsString = ""
    Else
        GetCellValueAsString = CStr(cell.Value)
    End If
    If Err.Number <> 0 Then
        GetCellValueAsString = ""
        Err.Clear
    End If
    On Error GoTo 0
End Function

' Function to sanitize filename
Function SanitizeFileName(name)
    Dim result
    result = name
    
    ' Replace invalid characters with underscores
    result = Replace(result, "\", "_")
    result = Replace(result, "/", "_")
    result = Replace(result, ":", "_")
    result = Replace(result, "*", "_")
    result = Replace(result, "?", "_")
    result = Replace(result, """", "_")
    result = Replace(result, "<", "_")
    result = Replace(result, ">", "_")
    result = Replace(result, "|", "_")
    
    SanitizeFileName = result
End Function

' Function to check if the file name starts with "ProgrammeReport"
Function IsProgrammeReport(fileName)
    If LCase(Left(fileName, 15)) = "programmereport" Then
        IsProgrammeReport = True
    Else
        IsProgrammeReport = False
    End If
End Function

' Function to modify workbook name (replace Programme with Module)
Function ModifyWorkbookName(originalName, isProgramme, isFirstTab)
    If isProgramme And Not isFirstTab Then
        ModifyWorkbookName = Replace(originalName, "Programme", "Module", 1, -1, vbTextCompare)
        ModifyWorkbookName = Replace(ModifyWorkbookName, "programme", "Module", 1, -1, vbTextCompare)
    Else
        ModifyWorkbookName = originalName
    End If
End Function

' Function to get the relative path of a file compared to a base folder
Function GetRelativePath(filePath, baseFolder)
    Dim relativePath
    
    ' Check if file is in the base folder directly
    If InStr(1, filePath, baseFolder, vbTextCompare) = 1 Then
        relativePath = Mid(filePath, Len(baseFolder) + 2) ' +2 to account for trailing backslash
        
        ' Get just the folder part (exclude the filename)
        If InStr(relativePath, "\") > 0 Then
            relativePath = Left(relativePath, InStrRev(relativePath, "\") - 1)
        Else
            relativePath = "" ' File is directly in the base folder
        End If
    Else
        relativePath = "" ' Default to root if not in base folder
    End If
    
    GetRelativePath = relativePath
End Function

' Function to check if worksheet contains "Report could not be retrieved"
Function ContainsReportNotRetrieved(ws)
    Dim r, c, cellVal
    ContainsReportNotRetrieved = False
    
    On Error Resume Next
    ' Check if UsedRange is valid
    If ws.UsedRange.Rows.Count > 0 And ws.UsedRange.Columns.Count > 0 Then
        ' Search in used range only for performance
        For r = 1 To ws.UsedRange.Rows.Count
            For c = 1 To ws.UsedRange.Columns.Count
                cellVal = GetCellValueAsString(ws.Cells(r, c))
                If InStr(1, cellVal, "Report could not be retrieved", vbTextCompare) > 0 Then
                    ContainsReportNotRetrieved = True
                    Exit Function
                End If
            Next
        Next
    End If
    On Error GoTo 0
End Function

' Function to process a single Excel file
Sub ProcessExcelFile(file)
    Dim fileProcessed, safeName, isProgramme, wsIndex, modifiedWorkbookName
    fileProcessed = False
    isProgramme = IsProgrammeReport(file.Name)
    
    On Error Resume Next ' Handle errors gracefully
    ' Open the workbook in read-only mode
    Set xlBook = xlApp.Workbooks.Open(file.Path, , True) ' The third parameter (True) opens the file in read-only mode
    
    If Err.Number <> 0 Then
        WScript.Echo "Error opening file: " & file.Name & ". Skipping..."
        Err.Clear
        On Error GoTo 0
        Exit Sub ' Exit the sub if there's an error opening the file
    End If
    On Error GoTo 0
    
    fileProcessed = True
    WScript.Echo "Starting to process file: " & file.Name
    
    ' Get workbook name without extension
    workbookName = Left(xlBook.Name, InStrRev(xlBook.Name, ".") - 1)
    
    ' Get the relative path to maintain folder structure
    relativePath = GetRelativePath(file.Path, folder)
    
    ' Create the target directory structure in csvData folder
    If relativePath <> "" Then
        saveFolderPath = fso.BuildPath(csvRootFolder, relativePath)
        ' Create the directory if it doesn't exist
        If Not fso.FolderExists(saveFolderPath) Then
            On Error Resume Next
            fso.CreateFolder(saveFolderPath)
            If Err.Number <> 0 Then
                WScript.Echo "Error creating folder: " & saveFolderPath & ". Error: " & Err.Description
                saveFolderPath = csvRootFolder ' Fall back to root csvData folder
                Err.Clear
            End If
            On Error GoTo 0
        End If
    Else
        saveFolderPath = csvRootFolder
    End If
    
    ' Loop through each worksheet in the workbook
    wsIndex = 1
    For Each ws In xlBook.Worksheets
        On Error Resume Next
        
        ' Check if worksheet contains "Report could not be retrieved"
        If ContainsReportNotRetrieved(ws) Then
            WScript.Echo "Worksheet contains 'Report could not be retrieved'. Skipping: " & ws.Name
            wsIndex = wsIndex + 1
            On Error GoTo 0
            ' Skip to the next worksheet
            WScript.Echo "Skipping worksheet: " & ws.Name
        Else
            ' Determine if this is the first tab for special handling
            Dim isFirstTab
            isFirstTab = (wsIndex = 1)
            
            ' Modify workbook name based on whether it's a ProgrammeReport file and if it's the first tab
            modifiedWorkbookName = ModifyWorkbookName(workbookName, isProgramme, isFirstTab)
            
            ' Sanitize worksheet name to create a valid filename
            safeName = SanitizeFileName(ws.Name)
            
            ' Construct file path for the CSV with modified workbook name if needed
            fileName = fso.BuildPath(saveFolderPath, SanitizeFileName(modifiedWorkbookName) & "_" & safeName & ".csv")
            
            ' Check if CSV file already exists
            If fso.FileExists(fileName) Then
                WScript.Echo "CSV file already exists for worksheet: " & ws.Name & ". Skipping..."
                ' Skip this worksheet and continue with the next one
            Else
                WScript.Echo "Processing worksheet: " & ws.Name
                
                ' Create a TextStream object for writing
                Set textStream = fso.CreateTextFile(fileName, True)
                
                If Err.Number <> 0 Then
                    WScript.Echo "Error creating CSV file for worksheet: " & ws.Name & ". Error: " & Err.Description
                    Err.Clear
                    ' Skip to next worksheet
                    On Error GoTo 0
                    WScript.Echo "Skipping worksheet: " & ws.Name
                Else
                    ' Find the last used row and column in the worksheet
                    lastRow = 1
                    lastCol = 1
                    
                    On Error Resume Next
                    ' Find the last row and column using UsedRange
                    If ws.UsedRange.Rows.Count > 0 Then
                        lastRow = ws.UsedRange.Rows.Count
                    End If
                    
                    If ws.UsedRange.Columns.Count > 0 Then
                        lastCol = ws.UsedRange.Columns.Count
                    End If
                    
                    ' Write sheet data to the CSV file
                                For rowNum = 1 To lastRow
                line = ""
                For colNum = 1 To lastCol
                    cellValue = GetCellValueAsString(ws.Cells(rowNum, colNum))

                    ' Clean up data
                    cellValue = Replace(cellValue, """", "")                  ' Remove all double quotes
                    cellValue = Replace(cellValue, ChrW(&H201C), "")         ' Left double quote
                    cellValue = Replace(cellValue, ChrW(&H201D), "")         ' Right double quote
                    cellValue = Replace(cellValue, ChrW(&H201A), "")         ' Single low-9 quote
                    cellValue = Replace(cellValue, ",", "")                  ' Remove all commas (thousands)
                    cellValue = Replace(cellValue, vbCr, "")
                    cellValue = Replace(cellValue, vbLf, "")
                    cellValue = Replace(cellValue, Chr(0), "")
                    cellValue = Trim(cellValue)

                    line = line & cellValue
                    If colNum < lastCol Then line = line & ","
                Next
                textStream.WriteLine line
            Next
            textStream.Close
                    
                    ' Close the TextStream
                    On Error Resume Next
                    textStream.Close
                    If Err.Number <> 0 Then
                        WScript.Echo "Error closing CSV file. Error: " & Err.Description
                        Err.Clear
                    End If
                    On Error GoTo 0
                    
                    WScript.Echo "Finished processing worksheet: " & ws.Name
                End If
            End If
            wsIndex = wsIndex + 1
            On Error GoTo 0
        End If
    Next
    
    ' Close the workbook without saving changes
    On Error Resume Next
    xlBook.Close False
    If Err.Number <> 0 Then
        WScript.Echo "Error closing workbook. Error: " & Err.Description
        Err.Clear
    End If
    On Error GoTo 0
    
    ' Print a message indicating the file has been processed
    WScript.Echo "Processed file: " & file.Name
End Sub

' Start processing from the main folder
On Error Resume Next
ProcessFolder folder

' Quit Excel
xlApp.Quit
If Err.Number <> 0 Then
    WScript.Echo "Error quitting Excel. Error: " & Err.Description
    Err.Clear
End If

' Kill Excel process to ensure it does not remain in the background
Dim shell
Set shell = CreateObject("WScript.Shell")
shell.Run "taskkill /F /IM excel.exe", 0, True

' Clean up
Set xlBook = Nothing
Set xlApp = Nothing
Set fso = Nothing
Set shell = Nothing

MsgBox "All Excel files have been processed. CSVs are saved in the csvData folder with preserved subfolder structure."