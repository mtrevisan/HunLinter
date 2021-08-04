if not exist "%WINDIR%/SysWOW64/richtx32.ocx" (
	copy richtx32.ocx %WINDIR%/SysWOW64
	cd %WINDIR%/SysWOW64
	rem regsvr32 richtx32.ocx
)
