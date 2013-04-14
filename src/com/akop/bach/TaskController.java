/*
 * TaskController.java
 * Copyright (C) 2010-2013 Akop Karapetyan
 *
 * This file is part of Spark 360, the online gaming service client.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */

package com.akop.bach;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Application;
import android.os.Process;

import com.akop.bach.parser.AuthenticationException;

public class TaskController implements Runnable
{
	private Object mListenerLock = new Object();
	
	private class TaskParameter
	{
		public Object result;
		
		public TaskParameter()
		{
			this.result = null;
		}
	}
	
	public static class TaskListener
	{
		private String mId;
		private boolean mActive;
		
		public TaskListener()
		{
			this(UUID.randomUUID().toString());
		}
		
		protected TaskListener(String id)
		{
			mId = id;
			mActive = false;
		}
		
		public String getId()
		{
			return mId;
		}
		
		public void onTaskStarted()
		{
		}
		
		public void onControllerBusy()
		{
		}
		
		public void onAllTasksCompleted()
		{
		}
		
		public void onAnyTaskFailed(int notified, Exception e)
		{
		}
		
		public void onTaskFailed(Account account, Exception e)
		{
		}
		
		public void onTaskSucceeded(Account account, Object requestParam, Object result)
		{
		}
		
		public void setActive(boolean active)
		{
			mActive = active;
		}
		
		public boolean isActive()
		{
			return mActive;
		}
	}
	
	public static abstract class CustomTask<T>
	{
		private T mResult;
		
		protected CustomTask()
		{
			mResult = null;
		}
		
		protected void setResult(T result)
		{
			mResult = result;
		}
		
		public T getResult()
		{
			return mResult;
		}
		
		public abstract void runTask() throws Exception;
	}
	
	private abstract class Task implements Runnable
	{
		public TaskListener listener;
		public String description;
		public boolean alwaysRun;
		public Account mAccount;
		public Object requestParam;
		public Exception taskError;
		public boolean isDisabled;
		
		public Task(String description, Account account, TaskListener listener)
		{
			this(description, account, listener, false, null);
		}
		
		public Task(String description, Account account, TaskListener listener, boolean alwaysRun)
		{
			this(description, account, listener, alwaysRun, null);
		}
		
		public Task(String description, Account account, TaskListener listener, boolean alwaysRun, Object requestParam)
		{
			this.isDisabled = false;
			this.mAccount = account;
			this.listener = listener;
			this.description = description;
			this.alwaysRun = alwaysRun;
			this.requestParam = requestParam;
			this.taskError = null;
		}
		
		@Override
		public void run()
		{
			TaskController tc = TaskController.getInstance();
			
			try
			{
				boolean callbackCalled = false;
				TaskParameter pm = new TaskParameter();
				execute(pm);
				
				synchronized(mListenerLock)
				{
					for (TaskListener l: tc.mListeners)
					{
						if (this.listener != null && l.getId().equals(this.listener.getId()))
						{
							l.onTaskSucceeded(this.mAccount, this.requestParam, pm.result);
							callbackCalled = true;
						}
					}
				}
				
				if (!callbackCalled && this.listener != null && this.alwaysRun)
				{
					try
					{
						this.listener.onTaskSucceeded(this.mAccount, this.requestParam, pm.result);
					}
					catch(Exception e)
					{
						if (App.getConfig().logToConsole())
							e.printStackTrace();
					}
				}
			}
			catch(Exception e)
			{
				boolean callbackCalled = false;
				this.taskError = e;
				
				if (App.getConfig().logToConsole())
					e.printStackTrace();
				
				synchronized (mListenerLock)
                {
					for (TaskListener l: tc.mListeners)
					{
						if (this.listener != null && l.getId().equals(this.listener.getId()))
						{
							l.onTaskFailed(this.mAccount, e);
							callbackCalled = true;
						}
					}
                }
				
				if (!callbackCalled && this.listener != null && this.alwaysRun)
				{
					try
					{
						this.listener.onTaskFailed(this.mAccount, e);
					}
					catch(Exception innerE)
					{
						if (App.getConfig().logToConsole())
							innerE.printStackTrace();
					}
				}
			}
		}
		
		protected abstract void execute(TaskParameter pm) throws Exception;
	}
	
    public HashSet<TaskListener> mListeners;
    
	private Thread mThread;
	private boolean mBusy;
	private Task mCurrentTask;
    private BlockingQueue<Task> mTasks;
    private static TaskController inst = null;
    private Application mApp;
    
	private TaskController()
	{
		mApp = App.getInstance();
		mTasks = new LinkedBlockingQueue<Task>();
		mListeners = new HashSet<TaskListener>();
		mCurrentTask = null;
		
		mThread = new Thread(this);
		mThread.start();
	}
	
	public static TaskController getInstance()
	{
		if (inst == null)
			inst = new TaskController();
		
		return inst;
	}

	public boolean isBusy()
	{
		return mBusy;
	}
	
	public void run()
	{
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		while (true)
		{
			try
			{
				mCurrentTask = mTasks.take(); // this call blocks
				
				boolean containsListener;
				
				synchronized(mListenerLock)
				{
					containsListener = mListeners.contains(mCurrentTask.listener);
				}
				
				if (mCurrentTask.alwaysRun 
						|| mCurrentTask.listener == null
						|| containsListener)
				{
					mBusy = true;
					
					// 'Started' callbacks
					synchronized(mListenerLock)
					{
						for (TaskListener l : mListeners)
						{
							l.onControllerBusy();
							l.onTaskStarted();
						}
					}
					
					// Run task
					if (!mCurrentTask.isDisabled)
						mCurrentTask.run();
					
					Exception error = mCurrentTask.taskError;
					if (error != null)
					{
						int notified = 0;
						
						synchronized(mListenerLock)
						{
							for (TaskListener l : mListeners)
								l.onAnyTaskFailed(notified++, error);
						}
						
						if (error instanceof AuthenticationException)
						{
							// In case of an authentication error, clear the task list, 
							// as all tasks are likely to fail
							
							if (App.getConfig().logToConsole())
								App.logv("Received authentication error - clearing controller's tasks");
							
							for (Task task : mTasks)
								task.isDisabled = true;
						}
					}
				}
				
				// 'Completed' callbacks
				if (mTasks.size() < 1)
				{
					synchronized(mListenerLock)
					{
						for (TaskListener l : mListeners)
							l.onAllTasksCompleted();
					}
				}
			}
			catch (Exception e)
			{
				if (App.getConfig().logToConsole())
				{
					App.logv("Error running command", e);
					e.printStackTrace();
				}
			}
			finally
			{
				mCurrentTask = null;
				mBusy = false;
			}
		}
	}
	
	private synchronized boolean addTask(Task newTask)
	{
		// Check for duplicate tasks
		if (mCurrentTask != null && mCurrentTask.description.equals(newTask.description))
		{
			mCurrentTask.listener = newTask.listener;
			
			if (App.getConfig().logToConsole())
				App.logv("Updating listener for current task '"
						+ mCurrentTask.description + "'");
			
			return true;
		}
		
		for (Task task : mTasks)
		{
			if (task.description.equals(newTask.description))
			{
				task.listener = newTask.listener;
				
				if (App.getConfig().logToConsole())
					App.logv("Updating listener for future task '" + task.description + "'");
				
				return true;
			}
		}
		
		// Add task
		try
		{
			mTasks.put(newTask);
		}
		catch (InterruptedException ie)
		{
			throw new Error(ie);
		}
		
		if (App.getConfig().logToConsole())
			App.logv("Controller added new task: " + newTask.description);
		
		return true;
	}

	public void addListener(TaskListener listener)
	{
		listener.setActive(true);
		
		synchronized (mListenerLock)
        {
			mListeners.add(listener);
        }
		
		if (this.isBusy())
			listener.onControllerBusy();
	}
	
	public void removeListener(TaskListener listener)
	{
		listener.setActive(false);
		
		synchronized (mListenerLock)
        {
			mListeners.remove(listener);
        }
	}
	
	public void validateAccount(final BasicAccount account, final TaskListener listener)
	{
		addTask(new Task("validateAccount:" + account.getUuid(), account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				pm.result = account.validate(mApp);
			}
		});
	}
	
	public void deleteAccount(final BasicAccount account, final TaskListener listener)
	{
		addTask(new Task("deleteAccount:" + account.getUuid(), account, listener, true) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.cleanUp(mApp);
			}
		});
	}
	
	public void synchronizeSummary(final BasicAccount account, final TaskListener listener)
	{
		addTask(new Task("synchronizeSummary:" + account.getUuid(), account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateProfile(mApp);
			}
		});
	}
	
	public void synchronizeGames(final SupportsGames account, final TaskListener listener)
	{
		addTask(new Task("synchronizeGames:" + account.getUuid(), account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateGames(mApp);
			}
		});
	}
	
	public void synchronizeAchievements(final SupportsAchievements account,
			final Object gameId,
			final TaskListener listener)
	{
		addTask(new Task("synchronizeAchievements:" + gameId, account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateAchievements(mApp, gameId);
			}
		});
	}
	
	public void updateFriendList(final SupportsFriends account,
			final TaskListener listener)
	{
		addTask(new Task("synchronizeFriends:" + account.getUuid(), account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateFriends(mApp);
			}
		});
	}
	
	public void updateFriendProfile(final SupportsFriends account,
			final Object friendId,
			final TaskListener listener)
	{
		addTask(new Task("synchronizeFriendSummary:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateFriendProfile(mApp, friendId);
			}
		});
	}
	
	public void addFriend(final SupportsFriendManagement account, 
			final Object friendId, 
			final Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("addFriend:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.addFriend(mApp, friendId);
			}
		});
	}
	
	public void removeFriend(final SupportsFriendManagement account, 
			final Object friendId, 
			final Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("removeFriend:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.removeFriend(mApp, friendId);
			}
		});
	}
	
	public void acceptFriendRequest(final SupportsFriendManagement account, 
			final Object friendId, 
			final Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("acceptFriend:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.acceptFriendRequest(mApp, friendId);
			}
		});
	}
	
	public void rejectFriendRequest(final SupportsFriendManagement account, 
			final Object friendId, 
			final Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("rejectFriend:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.rejectFriendRequest(mApp, friendId);
			}
		});
	}
	
	public void cancelFriendRequest(final SupportsFriendManagement account, 
			final Object friendId, 
			final Object requestParam, 
			final TaskListener listener)
	{
		addTask(new Task("cancelFriend:" + account.getUuid() + ":" + friendId.toString(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.cancelFriendRequest(mApp, friendId);
			}
		});
	}
	
	public void synchronizeMessages(final SupportsMessaging account,
			final TaskListener listener)
	{
		addTask(new Task("synchronizeMessages:" + account.getUuid(), account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateMessages(mApp);
			}
		});
	}
	
	public void synchronizeMessage(final SupportsMessaging account,
			final Object messageId,
			Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("synchronizeMessage:" + account.getUuid() + "," + messageId, 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.updateMessage(mApp, messageId);
			}
		});
	}
	
	public void deleteMessage(final SupportsMessaging account,
			final Object messageId,
			Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("deleteMessage:" + account.getUuid() + "," + messageId, 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.deleteMessage(mApp, messageId);
			}
		});
	}
	
	public void sendMessage(final SupportsMessaging account,
			final String[] recipients,
			final String body,
			Object requestParam,
			final TaskListener listener)
	{
		addTask(new Task("sendMessage:" + account.getUuid(), 
				account, listener, true, requestParam) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				account.sendMessage(mApp, recipients, body);
			}
		});
	}
	
	public void compareGames(final SupportsCompareGames account,
			final Object userId,
			final TaskListener listener)
	{
		addTask(new Task("compareGames:" + account.getUuid() + ":" + userId.toString(), 
				account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				pm.result = account.compareGames(mApp, userId);
			}
		});
	}
	
	public void compareAchievements(final SupportsCompareAchievements account,
			final Object userId,
			final Object gameId,
			final TaskListener listener)
	{
		addTask(new Task("compareAchievements:" + account.getUuid() + ":" + userId, 
				account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				pm.result = account.compareAchievements(mApp, userId, gameId);
			}
		});
	}
	
	public void runCustomTask(final BasicAccount account, final CustomTask<?> job,
			final TaskListener listener)
	{
		addTask(new Task("customTask:" + job, account, listener) 
		{
			@Override
			protected void execute(TaskParameter pm) throws Exception
			{
				job.runTask();
				pm.result = job.getResult();
			}
		});
	}
}
