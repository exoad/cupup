mod input_test

alias _Int as Int32
alias _Long as Int64
alias _Short as Int16
alias _Byte as Int8
alias _Float as Float32
alias _Double as Float64
alias _Bool as Bool
alias _Unit as Unit
alias _UInt as UInt32
alias _ULong as UInt64
alias _UByte as UInt8

fx demoDefer: Unit() {
    @stdio_printf("Starting function\n")
    defer {
        @stdio_printf("First defer executed\n")
    }
    defer {
        @stdio_printf("Second defer executed\n")
    }
    @stdio_printf("End of function\n")
}

mut x: Int32 = 10
while x > 0 {
    @stdio_printf("x = %d\n", x)
    x--

    if x == 5 {
        @stdio_printf("Breaking the loop at x = %d\n", x)
        break
    }
}