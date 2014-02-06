defmodule HelloJInterface do
  def pong do
    IO.puts "awaiting"
    receive do
      :stop -> IO.puts("Pong finished...")
      {_ping_id, :ping} ->
        IO.puts("Ping")
        pong()
    end
  end

  def start do
    :erlang.register(:pong, spawn(__MODULE__, :pong, []))
  end
end
