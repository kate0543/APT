Dim fso, folder, file, subfolder, xlApp, xlBook, ws
Dim saveFolderPath, workbookName, fileName
Dim rowNum, colNum, lastRow, lastCol, logSTR, cellValue, textStream
Dim csvRootFolder, relativePath, parentFolder

' Create FileSystemObject to handle folder and files
Set fso = CreateObject("Scripting.FileSystemObject")

' Get the folder where the script is located
folder = fso.GetParentFolderName(WScript.ScriptFullName)

' Get the parent folder of your original folder
parentFolder = fso.GetParentFolderName(folder)

' Build new path at same level with name "csvData"
csvRootFolder = fso.BuildPath(parentFolder, "csvData")

' Create csvData folder if it doesn't exist
If Not fso.FolderExists(csvRootFolder) Then
    fso.CreateFolder(csvRootFolder)
End If

' Create Excel application object
Set xlApp = CreateObject("Excel.Application")
xlApp.Visible = False

Sub CreateFolderPath(path)
    Dim parentFolder, folderParts, buildPath, i
    If fso.FolderExists(path) Then Exit Sub
    folderParts = Split(path, "\")
    buildPath = ""
    For i = 0 To UBound(folderParts)
        If i = 0 Then
            buildPath = folderParts(0) & "\"
        Else
            buildPath = buildPath & folderParts(i) & "\"
            If Not fso.FolderExists(buildPath) Then
                On Error Resume Next
                fso.CreateFolder(buildPath)
                Err.Clear
                On Error GoTo 0
            End If
        End If
    Next
End Sub

Sub ProcessFolder(currentFolder)
    For Each file In fso.GetFolder(currentFolder).Files
        If Left(file.Name, 2) = "~$" Then
            WScript.Echo "Skipping temporary file: " & file.Name
        ElseIf LCase(fso.GetExtensionName(file.Name)) = "xlsx" Then
            ProcessExcelFile file
        End If
    Next
    For Each subfolder In fso.GetFolder(currentFolder).SubFolders
        ProcessFolder subfolder
    Next
End Sub

Function GetCellValueAsString(cell)
    On Error Resume Next
    If IsEmpty(cell.Value) Or IsNull(cell.Value) Or IsObject(cell.Value) Then
        GetCellValueAsString = ""
    Else
        GetCellValueAsString = CStr(cell.Value)
    End If
    Err.Clear
    On Error GoTo 0
End Function

Function SanitizeFileName(name)
    Dim result
    result = name
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

Function IsProgrammeReport(fileName)
    IsProgrammeReport = (LCase(Left(fileName, 15)) = "programmereport")
End Function

Function ModifyWorkbookName(originalName, isProgramme, isFirstTab)
    If isProgramme And Not isFirstTab Then
        ModifyWorkbookName = Replace(originalName, "Programme", "Module", 1, -1, vbTextCompare)
    Else
        ModifyWorkbookName = originalName
    End If
End Function

Function GetRelativePath(filePath, baseFolder)
    If InStr(1, filePath, baseFolder, vbTextCompare) = 1 Then
        GetRelativePath = Mid(filePath, Len(baseFolder) + 2)
        If InStr(GetRelativePath, "\") > 0 Then
            GetRelativePath = Left(GetRelativePath, InStrRev(GetRelativePath, "\") - 1)
        Else
            GetRelativePath = ""
        End If
    Else
        GetRelativePath = ""
    End If
End Function

Function ContainsReportNotRetrieved(ws)
    Dim r, c, cellVal
    ContainsReportNotRetrieved = False
    On Error Resume Next
    For r = 1 To ws.UsedRange.Rows.Count
        For c = 1 To ws.UsedRange.Columns.Count
            cellVal = GetCellValueAsString(ws.Cells(r, c))
            If InStr(1, cellVal, "Report could not be retrieved", vbTextCompare) > 0 Then
                ContainsReportNotRetrieved = True
                Exit Function
            End If
        Next
    Next
    Err.Clear
    On Error GoTo 0
End Function

Sub ProcessExcelFile(file)
    Dim fileProcessed, safeName, isProgramme, wsIndex, modifiedWorkbookName
    fileProcessed = False
    isProgramme = IsProgrammeReport(file.Name)

    On Error Resume Next
    Set xlBook = xlApp.Workbooks.Open(file.Path, , True)
    If Err.Number <> 0 Then
        WScript.Echo "Error opening file: " & file.Name
        Err.Clear
        Exit Sub
    End If
    On Error GoTo 0

    workbookName = Left(xlBook.Name, InStrRev(xlBook.Name, ".") - 1)
    relativePath = GetRelativePath(file.Path, folder)

    If relativePath <> "" Then
        saveFolderPath = fso.BuildPath(csvRootFolder, relativePath)
        If Not fso.FolderExists(saveFolderPath) Then
            CreateFolderPath saveFolderPath
        End If
    Else
        saveFolderPath = csvRootFolder
    End If

    wsIndex = 1
    For Each ws In xlBook.Worksheets
        If ContainsReportNotRetrieved(ws) Then
            WScript.Echo "Skipping worksheet: " & ws.Name
        Else
            Dim isFirstTab
            isFirstTab = (wsIndex = 1)
            modifiedWorkbookName = ModifyWorkbookName(workbookName, isProgramme, isFirstTab)
            safeName = SanitizeFileName(ws.Name)
            fileName = fso.BuildPath(saveFolderPath, SanitizeFileName(modifiedWorkbookName) & "_" & safeName & ".csv")

            If fso.FileExists(fileName) Then
                WScript.Echo "CSV exists for worksheet: " & ws.Name
            Else
                Dim fileDir
                fileDir = fso.GetParentFolderName(fileName)
                If Not fso.FolderExists(fileDir) Then CreateFolderPath(fileDir)
                Set textStream = fso.CreateTextFile(fileName, True)

                If Err.Number = 0 Then
                    lastRow = ws.UsedRange.Rows.Count
                    lastCol = ws.UsedRange.Columns.Count
                    For rowNum = 1 To lastRow
                        logSTR = ""
                        For colNum = 1 To lastCol
                            cellValue = GetCellValueAsString(ws.Cells(rowNum, colNum))
                            cellValue = Replace(cellValue, Chr(34), "")
                            cellValue = Replace(cellValue, ChrW(&H201C), "")
                            cellValue = Replace(cellValue, ChrW(&H201D), "")
                            cellValue = Replace(cellValue, ChrW(&H201A), "")
                            cellValue = Trim(cellValue)
                            If InStr(cellValue, ",") > 0 Or InStr(cellValue, vbCrLf) > 0 Then
                                cellValue = """" & cellValue & """"
                            End If
                            logSTR = logSTR & cellValue
                            If colNum < lastCol Then logSTR = logSTR & ","
                        Next
                        
                        ' Add this sanity check to avoid invalid calls
On Error Resume Next
If IsNull(logSTR) Or IsEmpty(logSTR) Then
    textStream.WriteLine ""
Else
    logSTR = Replace(logSTR, vbCr, "") ' Clean carriage returns
    logSTR = Replace(logSTR, vbLf, "") ' Clean line feeds
    logSTR = Replace(logSTR, Chr(0), "") ' Remove null characters
    textStream.WriteLine CStr(logSTR)
End If
If Err.Number <> 0 Then
    WScript.Echo "Error writing to file: " & fileName & " at row " & rowNum & ". Skipping row."
    Err.Clear
End If
On Error GoTo 0

                    Next
                    textStream.Close
                Else
                    WScript.Echo "Error creating CSV for: " & ws.Name
                    Err.Clear
                End If
            End If
            wsIndex = wsIndex + 1
        End If
    Next

    xlBook.Close False
    On Error GoTo 0
    WScript.Echo "Processed: " & file.Name
End Sub

ProcessFolder folder

xlApp.Quit
If Err.Number <> 0 Then Err.Clear

Dim shell
Set shell = CreateObject("WScript.Shell")
shell.Run "taskkill /F /IM excel.exe", 0, True

Set xlBook = Nothing
Set xlApp = Nothing
Set fso = Nothing
Set shell = Nothing

MsgBox "All Excel files processed. CSVs saved in 'csvData' folder."
