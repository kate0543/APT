Dim fso, folder, file, excel, workbook, worksheet, cellValue
Dim programmeCode, termYear, levelYear, newName
Dim row, col, maxRows, maxCols
Dim oldName, newPath
Dim parts, tempCode, termCode

Set fso = CreateObject("Scripting.FileSystemObject")
Set excel = CreateObject("Excel.Application")
excel.Visible = False
excel.DisplayAlerts = False

Set folder = fso.GetFolder(fso.BuildPath(fso.GetParentFolderName(WScript.ScriptFullName), "SBMT/EBR"))

WScript.Echo "Processing folder: " & folder.Path

For Each file In folder.Files
    If LCase(fso.GetExtensionName(file.Name)) = "xlsx" Then
        WScript.Echo "Checking file: " & file.Name
        Set workbook = excel.Workbooks.Open(file.Path)
        Set worksheet = workbook.Sheets(1)

        programmeCode = ""
        termYear = ""
        levelYear = ""

        maxRows = 20  ' Adjust if needed
        maxCols = 10

        ' First pass to find Programme Code and Level
        For row = 1 To maxRows
            For col = 1 To maxCols
                cellValue = Trim(CStr(worksheet.Cells(row, col).Value))
                If cellValue <> "" Then 
                    WScript.Echo "Row " & row & ", Col " & col & ": " & cellValue
                End If

                ' Find Programme Code (LB/L/F or similar)
                If col = 2 And row >= 4 And row <= 6 Then
                    If InStr(cellValue, "/") > 0 Then
                        ' Convert something like "LB/L/F" to "LBL.F"
                        parts = Split(cellValue, "/")
                        If UBound(parts) >= 2 Then
                            programmeCode = Replace(parts(0), " ", "") & parts(1) & "." & parts(2)
                            WScript.Echo "Found programmeCode: " & programmeCode
                        End If
                    End If
                End If
                
                ' Term/Year (e.g., 202210 → 21-22)
                If col = 2 And row = 3 Then
                    termCode = Trim(cellValue)
                    If IsNumeric(termCode) And Len(termCode) = 6 Then
                        ' Extract year from term code (e.g., "22" from "202210")
                        yearEnd = Mid(termCode, 3, 2)
                        ' Calculate previous year (e.g., "21" from "22")
                        yearStart = Right("0" & CStr(CInt(yearEnd) - 1), 2)  ' Ensure 2 digits with leading zero
                        termYear = yearStart & "-" & yearEnd
                        WScript.Echo "Parsed termYear: " & termCode & " -> " & termYear
                    End If
                End If

                ' Programme Level (e.g., 4 → L4)
                If col = 2 And row = 7 Then
                    level = Trim(cellValue)
                    If IsNumeric(level) Then
                        levelYear = "L" & level
                        WScript.Echo "Parsed levelYear: " & levelYear
                    End If
                End If
            Next
        Next

        ' Rename if all data is present
        If programmeCode <> "" And termYear <> "" And levelYear <> "" Then
            newName = "ProgrammeReport-" & programmeCode & "-" & termYear & "-" & levelYear & ".xlsx"
            newPath = fso.BuildPath(folder.Path, newName)
            workbook.Close False
            
            ' Check if target file already exists before renaming
            If fso.FileExists(newPath) Then
                WScript.Echo "Warning: Target file already exists: " & newName
                WScript.Echo "Skipping rename operation for: " & file.Name
            Else
                fso.MoveFile file.Path, newPath
                WScript.Echo "Renamed to: " & newName
            End If
        Else
            workbook.Close False
            WScript.Echo "Skipped (missing data): " & file.Name & _
                " (programmeCode: " & programmeCode & ", termYear: " & termYear & ", levelYear: " & levelYear & ")"
        End If
    End If
Next

excel.Quit
Set excel = Nothing
Set fso = Nothing